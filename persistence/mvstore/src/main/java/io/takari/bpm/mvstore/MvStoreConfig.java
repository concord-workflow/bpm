package io.takari.bpm.mvstore;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

public final class MvStoreConfig {

    public static final int DEFAULT_CACHE_SIZE_MB = 16;
    public static final int DEFAULT_AUTO_COMPACT_FILL_RATE = 40;

    private final Path filePath;
    private final boolean syncWrite;
    private final boolean compress;
    private final boolean compressHigh;
    private final int cacheSizeMb;
    private final int autoCompactFillRate;
    private final boolean readOnly;
    private final Serializer serializer;
    private final Clock clock;

    private MvStoreConfig(Builder builder) {
        this.filePath = builder.filePath;
        this.syncWrite = builder.syncWrite;
        this.compress = builder.compress;
        this.compressHigh = builder.compressHigh;
        this.cacheSizeMb = builder.cacheSizeMb;
        this.autoCompactFillRate = builder.autoCompactFillRate;
        this.readOnly = builder.readOnly;
        this.serializer = builder.serializer;
        this.clock = builder.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder inMemory() {
        return new Builder().filePath(null);
    }

    public static Builder file(Path filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        return new Builder().filePath(filePath);
    }

    public Path getFilePath() {
        return filePath;
    }

    /**
     * When {@code true} (the default), every write commits and fsyncs to disk and
     * sets MVStore chunk retention time to 0 so freed chunks can be reused immediately.
     * When {@code false}, writes commit to the OS page cache without forcing fsync.
     */
    public boolean isSyncWrite() {
        return syncWrite;
    }

    public boolean isCompress() {
        return compress;
    }

    public boolean isCompressHigh() {
        return compressHigh;
    }

    public int getCacheSizeMb() {
        return cacheSizeMb;
    }

    /**
     * Target fill rate (0-100) used by MVStore when reclaiming space during
     * explicit {@link MvStoreDatabase#compact(int)} calls. Because MVStore
     * background auto-commit is disabled, callers should invoke
     * {@link MvStoreDatabase#compact(int)} periodically if file size matters.
     */
    public int getAutoCompactFillRate() {
        return autoCompactFillRate;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public Clock getClock() {
        return clock;
    }

    public static final class Builder {

        private Path filePath;
        private boolean syncWrite = true;
        private boolean compress;
        private boolean compressHigh;
        private int cacheSizeMb = DEFAULT_CACHE_SIZE_MB;
        private int autoCompactFillRate = DEFAULT_AUTO_COMPACT_FILL_RATE;
        private boolean readOnly;
        private Serializer serializer = new JavaSerializer();
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        public Builder filePath(Path filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder syncWrite(boolean syncWrite) {
            this.syncWrite = syncWrite;
            return this;
        }

        public Builder compress(boolean compress) {
            this.compress = compress;
            if (compress) {
                this.compressHigh = false;
            }
            return this;
        }

        public Builder compressHigh(boolean compressHigh) {
            this.compressHigh = compressHigh;
            if (compressHigh) {
                this.compress = false;
            }
            return this;
        }

        public Builder cacheSizeMb(int cacheSizeMb) {
            if (cacheSizeMb < 0) {
                throw new IllegalArgumentException("cacheSizeMb must be >= 0: " + cacheSizeMb);
            }
            this.cacheSizeMb = cacheSizeMb;
            return this;
        }

        public Builder autoCompactFillRate(int autoCompactFillRate) {
            if (autoCompactFillRate < 0 || autoCompactFillRate > 100) {
                throw new IllegalArgumentException("autoCompactFillRate must be between 0 and 100: " + autoCompactFillRate);
            }
            this.autoCompactFillRate = autoCompactFillRate;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder serializer(Serializer serializer) {
            this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
            return this;
        }

        public MvStoreConfig build() {
            if (readOnly && filePath == null) {
                throw new IllegalArgumentException("readOnly cannot be enabled for an in-memory store");
            }
            return new MvStoreConfig(this);
        }
    }
}
