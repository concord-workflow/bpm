package io.takari.bpm;

import io.takari.bpm.api.Engine;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.EventStorage;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.handlers.DelegatingElementHandler;
import io.takari.bpm.handlers.ElementHandler;
import io.takari.bpm.lock.LockManager;
import io.takari.bpm.lock.StripedLockManagerImpl;
import io.takari.bpm.persistence.DummyPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.task.ServiceTaskRegistry;

public final class EngineBuilder {

    private static final int DEFAULT_CONCURRENCY_LEVEL = 64;
    
    private ProcessDefinitionProvider definitionProvider;
    private ElementHandler elementHandler;
    private EventPersistenceManager eventManager;
    private PersistenceManager persistenceManager;
    private ServiceTaskRegistry taskRegistry;
    private ExpressionManager expressionManager;
    private LockManager lockManager;
    private UuidGenerator uuidGenerator;
    
    public EngineBuilder withDefinitionProvider(ProcessDefinitionProvider definitionProvider) {
        this.definitionProvider = definitionProvider;
        return this;
    }
    
    public EngineBuilder withEventManager(EventPersistenceManager eventManager) {
        this.eventManager = eventManager;
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
            persistenceManager = new DummyPersistenceManager();
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
        
        return new EngineImpl(new IndexedProcessDefinitionProvider(definitionProvider),
                elementHandler, eventManager, persistenceManager, taskRegistry,
                expressionManager, lockManager, uuidGenerator);
    }
    
    public static final class EngineImpl extends AbstractEngine {

        private final IndexedProcessDefinitionProvider definitionProvider;
        private final ElementHandler elementHandler;
        private final EventPersistenceManager eventManager;
        private final PersistenceManager persistenceManager;
        private final ServiceTaskRegistry taskRegistry;
        private final ExpressionManager expressionManager;
        private final LockManager lockManager;
        private final UuidGenerator uuidGenerator;

        public EngineImpl(
                IndexedProcessDefinitionProvider definitionProvider,
                ElementHandler elementHandler,
                EventPersistenceManager eventManager,
                PersistenceManager persistenceManager,
                ServiceTaskRegistry taskRegistry,
                ExpressionManager expressionManager,
                LockManager lockManager,
                UuidGenerator uuidGenerator) {
            
            // TODO cyclic dependency
            this.elementHandler = elementHandler == null ? new DelegatingElementHandler(this) : elementHandler;
            
            this.definitionProvider = definitionProvider;
            this.eventManager = eventManager;
            this.persistenceManager = persistenceManager;
            this.taskRegistry = taskRegistry;
            this.expressionManager = expressionManager;
            this.lockManager = lockManager;
            this.uuidGenerator = uuidGenerator;
        }
        
        @Override
        public IndexedProcessDefinitionProvider getProcessDefinitionProvider() {
            return definitionProvider;
        }

        @Override
        public ElementHandler getElementHandler() {
            return elementHandler;
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
        public ServiceTaskRegistry getServiceTaskRegistry() {
            return taskRegistry;
        }

        @Override
        public ExpressionManager getExpressionManager() {
            return expressionManager;
        }

        @Override
        public LockManager getLockManager() {
            return lockManager;
        }

        @Override
        public UuidGenerator getUuidGenerator() {
            return uuidGenerator;
        }
    }
}
