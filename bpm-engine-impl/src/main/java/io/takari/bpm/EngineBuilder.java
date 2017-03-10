package io.takari.bpm;

import io.takari.bpm.api.Engine;
import io.takari.bpm.api.EventService;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.*;
import io.takari.bpm.lock.LockManager;
import io.takari.bpm.lock.StripedLockManagerImpl;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.planner.DefaultPlanner;
import io.takari.bpm.planner.Planner;
import io.takari.bpm.task.NoopUserTaskHandler;
import io.takari.bpm.task.ServiceTaskRegistry;
import io.takari.bpm.task.UserTaskHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public final class EngineBuilder {

    private static final int DEFAULT_CONCURRENCY_LEVEL = 64;

    private EventStorage eventStorage;
    private EventPersistenceManager eventManager;
    private ExecutionInterceptorHolder interceptors;
    private Executor executor;
    private ExecutorService threadPool;
    private ExpressionManager expressionManager;
    private LockManager lockManager;
    private PersistenceManager persistenceManager;
    private Planner planner;
    private ProcessDefinitionProvider definitionProvider;
    private Function<ProcessDefinitionProvider, IndexedProcessDefinitionProvider> definitionProviderWrappingFn;
    private ServiceTaskRegistry taskRegistry;
    private UuidGenerator uuidGenerator;
    private Function<Executor, Executor> executorWrappingFn;
    private UserTaskHandler userTaskHandler;
    private Configuration configuration;

    public EngineBuilder withDefinitionProvider(ProcessDefinitionProvider definitionProvider) {
        this.definitionProvider = definitionProvider;
        return this;
    }

    public EngineBuilder withEventStorage(EventStorage eventStorage) {
        this.eventStorage = eventStorage;
        return this;
    }

    public EngineBuilder withEventManager(EventPersistenceManager eventManager) {
        this.eventManager = eventManager;
        return this;
    }

    public EngineBuilder withExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public EngineBuilder withTaskRegistry(ServiceTaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
        return this;
    }

    public EngineBuilder withPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
        return this;
    }

    public EngineBuilder withLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
        return this;
    }

    public EngineBuilder withPlanner(Planner planner) {
        this.planner = planner;
        return this;
    }

    public EngineBuilder withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public EngineBuilder withThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
        return this;
    }

    public EngineBuilder wrapExecutorWith(Function<Executor, Executor> fn) {
        this.executorWrappingFn = fn;
        return this;
    }

    public EngineBuilder withConfiguration(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    public EngineBuilder wrapDefinitionProviderWith(Function<ProcessDefinitionProvider, IndexedProcessDefinitionProvider> fn) {
        this.definitionProviderWrappingFn = fn;
        return this;
    }

    public EngineBuilder withUserTaskHandler(UserTaskHandler userTaskHandler) {
        this.userTaskHandler = userTaskHandler;
        return this;
    }

    public Engine build() {
        if (configuration == null) {
            configuration = new Configuration();
        }

        if (definitionProvider == null) {
            throw new IllegalStateException("Process definition provider is required. "
                    + "Use the method `builder.withDefinitionProvider(...)` to specify your own implementation.");
        }

        if (eventStorage == null) {
            eventStorage = new InMemEventStorage();
        }

        if (eventManager == null) {
            eventManager = new EventPersistenceManagerImpl(eventStorage);
        }

        if (persistenceManager == null) {
            persistenceManager = new InMemPersistenceManager();
        }

        if (taskRegistry == null) {
            throw new IllegalStateException("Service task registry is required. "
                    + "Use the method `builder.withTaskRegistry(...)` to specify your own implementation.");
        }

        if (expressionManager == null) {
            expressionManager = new DefaultExpressionManager(taskRegistry);
        }

        if (lockManager == null) {
            lockManager = new StripedLockManagerImpl(DEFAULT_CONCURRENCY_LEVEL);
        }

        if (uuidGenerator == null) {
            uuidGenerator = new RandomUuidGenerator();
        }

        if (planner == null) {
            planner = new DefaultPlanner(configuration);
        }

        if (interceptors == null) {
            interceptors = new ExecutionInterceptorHolder();
        }

        if (threadPool == null) {
            threadPool = Executors.newCachedThreadPool();
        }

        IndexedProcessDefinitionProvider indexedDefinitionProvider;
        if (definitionProviderWrappingFn != null) {
            indexedDefinitionProvider = definitionProviderWrappingFn.apply(definitionProvider);
        } else {
            indexedDefinitionProvider = new IndexedProcessDefinitionProvider(definitionProvider);
        }

        if (userTaskHandler == null) {
            userTaskHandler = new NoopUserTaskHandler();
        }

        if (executor == null) {
            executor = new DefaultExecutor(configuration, expressionManager, threadPool, interceptors,
                    indexedDefinitionProvider, uuidGenerator, eventManager, persistenceManager,
                    userTaskHandler);
        }

        if (executorWrappingFn != null) {
            executor = executorWrappingFn.apply(executor);
        }

        return new EngineImpl(new IndexedProcessDefinitionProvider(definitionProvider),
                eventStorage, eventManager,
                persistenceManager, lockManager, uuidGenerator,
                executor, planner,
                userTaskHandler, configuration, interceptors);
    }

    public static final class EngineImpl extends AbstractEngine {

        private final IndexedProcessDefinitionProvider definitionProvider;
        private final EventPersistenceManager eventManager;
        private final PersistenceManager persistenceManager;
        private final LockManager lockManager;
        private final UuidGenerator uuidGenerator;
        private final Executor executor;
        private final Planner planner;
        private final UserTaskHandler userTaskHandler;
        private final Configuration configuration;
        private final ExecutionInterceptorHolder interceptors;

        private final EventService eventService;

        public EngineImpl(
                IndexedProcessDefinitionProvider definitionProvider,
                EventStorage eventStorage,
                EventPersistenceManager eventManager,
                PersistenceManager persistenceManager,
                LockManager lockManager,
                UuidGenerator uuidGenerator,
                Executor executor,
                Planner planner,
                UserTaskHandler userTaskHandler, Configuration configuration,
                ExecutionInterceptorHolder interceptors) {

            this.definitionProvider = definitionProvider;
            this.eventManager = eventManager;
            this.persistenceManager = persistenceManager;
            this.lockManager = lockManager;
            this.uuidGenerator = uuidGenerator;

            this.executor = executor;
            this.planner = planner;
            this.userTaskHandler = userTaskHandler;
            this.configuration = configuration;

            this.interceptors = interceptors;

            this.eventService = new EventServiceImpl(lockManager, eventStorage);
        }

        @Override
        public IndexedProcessDefinitionProvider getProcessDefinitionProvider() {
            return definitionProvider;
        }

        @Override
        public EventPersistenceManager getEventManager() {
            return eventManager;
        }

        @Override
        public PersistenceManager getPersistenceManager() {
            return persistenceManager;
        }

        @Override
        public LockManager getLockManager() {
            return lockManager;
        }

        @Override
        public UuidGenerator getUuidGenerator() {
            return uuidGenerator;
        }

        @Override
        protected ExecutionInterceptorHolder getInterceptorHolder() {
            return interceptors;
        }

        @Override
        protected Planner getPlanner() {
            return planner;
        }

        @Override
        protected Executor getExecutor() {
            return executor;
        }

        @Override
        public Configuration getConfiguration() {
            return configuration;
        }

        @Override
        public EventService getEventService() {
            return eventService;
        }

        @Override
        public UserTaskHandler getUserTaskHandler() {
            return userTaskHandler;
        }
    }
}
