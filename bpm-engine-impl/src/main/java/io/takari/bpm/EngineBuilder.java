package io.takari.bpm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import io.takari.bpm.api.Engine;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.EventStorage;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.lock.LockManager;
import io.takari.bpm.lock.StripedLockManagerImpl;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.planner.DefaultPlanner;
import io.takari.bpm.planner.Planner;
import io.takari.bpm.task.ServiceTaskRegistry;

public final class EngineBuilder {

    private static final int DEFAULT_CONCURRENCY_LEVEL = 64;
    
    private EventPersistenceManager eventManager;
    private ExecutionInterceptorHolder interceptors;
    private Executor executor;
    private ExecutorService threadPool;
    private ExpressionManager expressionManager;
    private LockManager lockManager;
    private PersistenceManager persistenceManager;
    private Planner planner;
    private ProcessDefinitionProvider definitionProvider;
    private ServiceTaskRegistry taskRegistry;
    private UuidGenerator uuidGenerator;
    private Function<Executor, Executor> executorWrappingFn;

    public EngineBuilder withDefinitionProvider(ProcessDefinitionProvider definitionProvider) {
        this.definitionProvider = definitionProvider;
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

    public EngineBuilder wrapExecutorWith(Function<Executor, Executor> fn) {
        this.executorWrappingFn = fn;
        return this;
    }
    
    public Engine build() {
        if (definitionProvider == null) {
            throw new IllegalStateException("Process definition provider is required. "
                    + "Use the method `builder.withDefinitionProvider(...)` to specify your own implementation.");
        }
        
        if (eventManager == null) {
            EventStorage es = new InMemEventStorage();
            eventManager = new EventPersistenceManagerImpl(es);
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
            planner = new DefaultPlanner();
        }

        if (interceptors == null) {
            interceptors = new ExecutionInterceptorHolder();
        }

        if (threadPool == null) {
            threadPool = Executors.newCachedThreadPool();
        }

        IndexedProcessDefinitionProvider indexedDefinitionProvider = new IndexedProcessDefinitionProvider(definitionProvider);

        if (executor == null) {
            executor = new DefaultExecutor(expressionManager, threadPool, interceptors, indexedDefinitionProvider, uuidGenerator,
                    eventManager, persistenceManager);
        }

        if (executorWrappingFn != null) {
            executor = executorWrappingFn.apply(executor);
        }

        return new EngineImpl(new IndexedProcessDefinitionProvider(definitionProvider),
                eventManager, persistenceManager, lockManager, uuidGenerator, executor, planner, interceptors);
    }
    
    public static final class EngineImpl extends AbstractEngine {

        private final IndexedProcessDefinitionProvider definitionProvider;
        private final EventPersistenceManager eventManager;
        private final PersistenceManager persistenceManager;
        private final LockManager lockManager;
        private final UuidGenerator uuidGenerator;
        private final Executor executor;
        private final Planner planner;
        private final ExecutionInterceptorHolder interceptors;

        public EngineImpl(
                IndexedProcessDefinitionProvider definitionProvider,
                EventPersistenceManager eventManager,
                PersistenceManager persistenceManager,
                LockManager lockManager,
                UuidGenerator uuidGenerator,
                Executor executor,
                Planner planner,
                ExecutionInterceptorHolder interceptors) {
            
            this.definitionProvider = definitionProvider;
            this.eventManager = eventManager;
            this.persistenceManager = persistenceManager;
            this.lockManager = lockManager;
            this.uuidGenerator = uuidGenerator;

            this.executor = executor;
            this.planner = planner;
            this.interceptors = interceptors;
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
    }
}
