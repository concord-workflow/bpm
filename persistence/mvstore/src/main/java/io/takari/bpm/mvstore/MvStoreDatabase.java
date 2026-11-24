package io.takari.bpm.mvstore;

import io.takari.bpm.mvstore.StoreTypes.BusinessKeyIndexKey;
import io.takari.bpm.mvstore.StoreTypes.BusinessKeyIndexKeyDataType;
import io.takari.bpm.mvstore.StoreTypes.ExpiredEventIndexKey;
import io.takari.bpm.mvstore.StoreTypes.ExpiredEventIndexKeyDataType;
import io.takari.bpm.mvstore.StoreTypes.Unit;
import io.takari.bpm.mvstore.StoreTypes.UnitDataType;
import io.takari.bpm.mvstore.StoreTypes.UuidDataType;
import org.h2.mvstore.FileStore;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreException;
import org.h2.mvstore.type.ByteArrayDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Embedded H2 MVStore database backing {@link MvStorePersistenceManager} and {@link MvStoreEventStorage}.
 * <p>
 * All MVStore I/O runs sequentially on a dedicated non-interruptible daemon thread so caller
 * thread interrupts (e.g. during scheduler shutdown or task cancellation) cannot trigger
 * {@link java.nio.channels.ClosedByInterruptException} on the underlying file channel.
 * Serialization and deserialization run on caller threads outside the I/O thread.
 */
public final class MvStoreDatabase implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MvStoreDatabase.class);
    private static final AtomicInteger IO_THREAD_SEQ = new AtomicInteger();

    static final int STORE_VERSION = 1;

    static final String EXECUTIONS_MAP = "executions";
    static final String EVENTS_MAP = "events";
    static final String EVENTS_BY_KEY_MAP = "events_by_key";
    static final String EXPIRED_EVENTS_MAP = "expired_events";

    private final MvStoreConfig config;
    private final ExecutorService ioExecutor;
    private final Object lifecycleLock = new Object();

    private final MVStore store;
    private final MVMap<UUID, byte[]> executionsMap;
    private final MVMap<UUID, byte[]> eventsMap;
    private final MVMap<BusinessKeyIndexKey, Unit> businessKeyIndex;
    private final MVMap<ExpiredEventIndexKey, Unit> expiredEventsIndex;

    private final MvStorePersistenceManager persistenceManager;
    private final MvStoreEventStorage eventStorage;

    private volatile boolean closed;
    private Future<Void> closeFuture;

    private MvStoreDatabase(MvStoreConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "mvstore-io-" + IO_THREAD_SEQ.incrementAndGet());
            t.setDaemon(true);
            return t;
        });

        InitializedStore initialized;
        try {
            initialized = awaitUninterruptibly(ioExecutor.submit(() -> initializeStore(config)));
        } catch (Throwable t) {
            ioExecutor.shutdownNow();
            throw t;
        }

        this.store = initialized.store;
        this.executionsMap = initialized.executionsMap;
        this.eventsMap = initialized.eventsMap;
        this.businessKeyIndex = initialized.businessKeyIndex;
        this.expiredEventsIndex = initialized.expiredEventsIndex;

        this.persistenceManager = new MvStorePersistenceManager(this);
        this.eventStorage = new MvStoreEventStorage(this);
    }

    public static MvStoreDatabase open(MvStoreConfig config) {
        return new MvStoreDatabase(config);
    }

    public MvStorePersistenceManager persistenceManager() {
        return persistenceManager;
    }

    public MvStoreEventStorage eventStorage() {
        return eventStorage;
    }

    /**
     * Reclaims unused space in the backing file, spending up to {@code maxCompactTimeMillis}.
     * No-op for in-memory stores.
     */
    public void compact(int maxCompactTimeMillis) {
        if (maxCompactTimeMillis < 0) {
            throw new IllegalArgumentException("maxCompactTimeMillis must be >= 0: " + maxCompactTimeMillis);
        }

        submitAndAwait(() -> {
            ensureStoreOpenOnIoThread();
            if (config.isReadOnly()) {
                throw new PersistenceException("Cannot compact a read-only MVStore database");
            }
            if (store.isPersistent()) {
                int prevRetention = store.getRetentionTime();
                try {
                    store.compactFile(maxCompactTimeMillis);
                    if (config.isSyncWrite()) {
                        store.sync();
                    }
                } catch (RuntimeException e) {
                    if (store.isClosed()) {
                        closed = true;
                    }
                    throw wrapStoreFailure("MVStore compaction failed", e);
                } finally {
                    if (!store.isClosed()) {
                        store.setRetentionTime(prevRetention);
                    }
                }
            }
            return null;
        });
    }

    public boolean isClosed() {
        return closed || store.isClosed();
    }

    @Override
    public void close() {
        Future<Void> future;
        boolean firstCaller = false;
        synchronized (lifecycleLock) {
            if (closeFuture != null) {
                future = closeFuture;
            } else {
                closed = true;
                firstCaller = true;
                future = ioExecutor.submit(() -> {
                    closeStoreOnIoThread();
                    return null;
                });
                closeFuture = future;
                ioExecutor.shutdown();
            }
        }

        if (firstCaller) {
            awaitUninterruptibly(future);
        } else {
            // Wait for the in-progress close to finish without re-throwing a prior close error.
            try {
                awaitUninterruptibly(future);
            } catch (RuntimeException ignored) {
                // Already reported to the caller that initiated close().
            }
        }
    }

    /**
     * Package-private hook for simulating an abrupt process crash in tests
     * (closes the store without writing a clean-shutdown footer).
     */
    void closeImmediatelyForTest() {
        Future<Void> future;
        synchronized (lifecycleLock) {
            if (closeFuture != null) {
                future = closeFuture;
            } else {
                closed = true;
                future = ioExecutor.submit(() -> {
                    store.closeImmediately();
                    return null;
                });
                closeFuture = future;
                ioExecutor.shutdown();
            }
        }
        awaitUninterruptibly(future);
    }

    int getRetentionTimeForTest() {
        return read(store::getRetentionTime);
    }

    Serializer getSerializer() {
        return config.getSerializer();
    }

    Clock getClock() {
        return config.getClock();
    }

    MVMap<UUID, byte[]> executionsMap() {
        return executionsMap;
    }

    MVMap<UUID, byte[]> eventsMap() {
        return eventsMap;
    }

    MVMap<BusinessKeyIndexKey, Unit> businessKeyIndex() {
        return businessKeyIndex;
    }

    MVMap<ExpiredEventIndexKey, Unit> expiredEventsIndex() {
        return expiredEventsIndex;
    }

    <T> T read(Supplier<T> action) {
        return submitAndAwait(() -> {
            ensureStoreOpenOnIoThread();
            try {
                return action.get();
            } catch (PersistenceException pe) {
                throw pe;
            } catch (RuntimeException e) {
                if (store.isClosed()) {
                    closed = true;
                }
                throw wrapStoreFailure("MVStore read operation failed", e);
            }
        });
    }

    <T> T write(Supplier<T> action) {
        return submitAndAwait(() -> {
            ensureStoreOpenOnIoThread();
            if (config.isReadOnly()) {
                throw new PersistenceException("MVStore database is opened in read-only mode");
            }
            long versionBefore = store.getCurrentVersion();
            boolean committed = false;
            try {
                T result = action.get();
                if (store.hasUnsavedChanges()) {
                    store.commit();
                    committed = true;
                    if (config.isSyncWrite() && store.isPersistent()) {
                        store.sync();
                    }
                } else {
                    committed = true;
                }
                return result;
            } catch (Throwable t) {
                if (!committed) {
                    rollbackOrFail(versionBefore, t);
                }
                if (store.isClosed()) {
                    closed = true;
                    throw wrapStoreFailure("MVStore write failed and store was closed", t);
                }
                if (t instanceof PersistenceException pe) {
                    throw pe;
                }
                if (t instanceof Error err) {
                    throw err;
                }
                throw wrapStoreFailure("MVStore write operation failed", t);
            }
        });
    }

    private <T> T submitAndAwait(Supplier<T> task) {
        Future<T> future;
        synchronized (lifecycleLock) {
            if (closed || store.isClosed()) {
                throw new IllegalStateException("MVStore database is closed");
            }
            try {
                future = ioExecutor.submit(task::get);
            } catch (RejectedExecutionException e) {
                throw new IllegalStateException("MVStore database is closed", e);
            }
        }
        return awaitUninterruptibly(future);
    }

    private void ensureStoreOpenOnIoThread() {
        if (store.isClosed()) {
            closed = true;
            MVStoreException panic = store.getPanicException();
            if (panic != null) {
                throw new PersistenceException("MVStore database panicked and was closed", panic);
            }
            throw new IllegalStateException("MVStore database is closed");
        }
    }

    private void closeStoreOnIoThread() {
        if (store.isClosed()) {
            return;
        }
        try {
            if (!config.isReadOnly() && store.hasUnsavedChanges()) {
                store.commit();
                if (config.isSyncWrite() && store.isPersistent()) {
                    store.sync();
                }
            }
            store.close();
        } catch (RuntimeException e) {
            try {
                store.closeImmediately();
            } catch (RuntimeException closeErr) {
                if (closeErr != e) {
                    e.addSuppressed(closeErr);
                }
            }
            throw wrapStoreFailure("Failed to close MVStore database", e);
        }
    }

    private void rollbackOrFail(long versionBefore, Throwable original) {
        if (store.isClosed()) {
            closed = true;
            return;
        }
        try {
            store.rollbackTo(versionBefore);
        } catch (Throwable rollbackErr) {
            if (rollbackErr != original) {
                original.addSuppressed(rollbackErr);
            }
            closed = true;
            try {
                store.closeImmediately();
            } catch (Throwable closeErr) {
                if (closeErr != original && closeErr != rollbackErr) {
                    original.addSuppressed(closeErr);
                }
            }
        }
    }

    private PersistenceException wrapStoreFailure(String message, Throwable cause) {
        MVStoreException panic = store.getPanicException();
        if (panic != null && panic != cause) {
            if (cause == null) {
                return new PersistenceException(message, panic);
            }
            cause.addSuppressed(panic);
        }
        return new PersistenceException(message, cause);
    }

    private static <T> T awaitUninterruptibly(Future<T> future) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Error err) {
                        throw err;
                    }
                    if (cause instanceof IllegalStateException ise) {
                        throw ise;
                    }
                    if (cause instanceof PersistenceException pe) {
                        throw pe;
                    }
                    throw new PersistenceException("MVStore operation failed", cause);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private record InitializedStore(
            MVStore store,
            MVMap<UUID, byte[]> executionsMap,
            MVMap<UUID, byte[]> eventsMap,
            MVMap<BusinessKeyIndexKey, Unit> businessKeyIndex,
            MVMap<ExpiredEventIndexKey, Unit> expiredEventsIndex) {
    }

    private static InitializedStore initializeStore(MvStoreConfig config) {
        MVStore store = openStore(config);
        try {
            if (!config.isReadOnly()) {
                FileStore<?> fileStore = store.getFileStore();
                if (store.isReadOnly() || (fileStore != null && fileStore.isReadOnly())) {
                    throw new PersistenceException("MVStore file is read-only: " + config.getFilePath());
                }
                if (config.isSyncWrite() && store.isPersistent()) {
                    store.setRetentionTime(0);
                }
                store.setStoreVersion(STORE_VERSION);
            }

            MVMap<UUID, byte[]> executionsMap = store.openMap(EXECUTIONS_MAP,
                    new MVMap.Builder<UUID, byte[]>()
                            .keyType(UuidDataType.INSTANCE)
                            .valueType(ByteArrayDataType.INSTANCE));

            MVMap<UUID, byte[]> eventsMap = store.openMap(EVENTS_MAP,
                    new MVMap.Builder<UUID, byte[]>()
                            .keyType(UuidDataType.INSTANCE)
                            .valueType(ByteArrayDataType.INSTANCE));

            MVMap<BusinessKeyIndexKey, Unit> businessKeyIndex = store.openMap(EVENTS_BY_KEY_MAP,
                    new MVMap.Builder<BusinessKeyIndexKey, Unit>()
                            .keyType(BusinessKeyIndexKeyDataType.INSTANCE)
                            .valueType(UnitDataType.INSTANCE));

            MVMap<ExpiredEventIndexKey, Unit> expiredEventsIndex = store.openMap(EXPIRED_EVENTS_MAP,
                    new MVMap.Builder<ExpiredEventIndexKey, Unit>()
                            .keyType(ExpiredEventIndexKeyDataType.INSTANCE)
                            .valueType(UnitDataType.INSTANCE));

            if (!config.isReadOnly() && store.hasUnsavedChanges()) {
                store.commit();
                if (config.isSyncWrite() && store.isPersistent()) {
                    store.sync();
                }
            }

            return new InitializedStore(store, executionsMap, eventsMap, businessKeyIndex, expiredEventsIndex);
        } catch (RuntimeException e) {
            closeQuietly(store);
            if (e instanceof PersistenceException pe) {
                throw pe;
            }
            throw new PersistenceException("Failed to initialize MVStore maps", e);
        }
    }

    private static MVStore openStore(MvStoreConfig config) {
        Path filePath = config.getFilePath();
        if (filePath != null && !config.isReadOnly()) {
            Path parent = filePath.toAbsolutePath().getParent();
            if (parent != null) {
                try {
                    Files.createDirectories(parent);
                } catch (IOException e) {
                    throw new PersistenceException("Failed to create parent directory: " + parent, e);
                }
            }
        }

        MVStore.Builder builder = new MVStore.Builder()
                .autoCommitDisabled()
                .autoCommitBufferSize(0)
                .cacheSize(config.getCacheSizeMb())
                .autoCompactFillRate(config.getAutoCompactFillRate())
                .backgroundExceptionHandler((thread, ex) ->
                        log.debug("MVStore panic on thread {}", thread.getName(), ex));

        if (filePath != null) {
            builder.fileName(filePath.toAbsolutePath().toString());
        }
        if (config.isReadOnly()) {
            builder.readOnly();
        }
        if (config.isCompressHigh()) {
            builder.compressHigh();
        } else if (config.isCompress()) {
            builder.compress();
        }

        try {
            return builder.open();
        } catch (RuntimeException e) {
            throw new PersistenceException("Failed to open MVStore database" + (filePath != null ? ": " + filePath : ""), e);
        }
    }

    private static void closeQuietly(MVStore store) {
        try {
            store.closeImmediately();
        } catch (RuntimeException e) {
            log.warn("Failed to close MVStore after initialization error", e);
        }
    }
}
