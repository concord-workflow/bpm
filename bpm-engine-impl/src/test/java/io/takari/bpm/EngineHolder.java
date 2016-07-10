package io.takari.bpm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecutor;
import io.takari.bpm.ExecutionInterceptorHolder;
import io.takari.bpm.Executor;
import io.takari.bpm.IndexedProcessDefinitionProvider;
import io.takari.bpm.RandomUuidGenerator;
import io.takari.bpm.UuidGenerator;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.interceptors.ElementEvent;
import io.takari.bpm.api.interceptors.ExecutionInterceptorAdapter;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.lock.LockManager;
import io.takari.bpm.lock.SingleLockManagerImpl;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.planner.DefaultPlanner;
import io.takari.bpm.planner.Planner;
import io.takari.bpm.task.ServiceTaskRegistryImpl;

public class EngineHolder {

    private final ServiceTaskRegistryImpl serviceTaskRegistry;
    private final TestProcessDefinitionProvider processDefinitionProvider;
    private final IndexedProcessDefinitionProvider indexedProcessDefinitionProvider;
    private final AbstractEngine engine;
    private final Map<String, List<String>> activations;
    private final EventPersistenceManager eventManager;
    private final PersistenceManager persistenceManager;
    private final Executor executor;
    private final ExpressionManager expressionManager;
    private final ExecutionInterceptorHolder interceptorHolder = new ExecutionInterceptorHolder();
    private final UuidGenerator uuidGenerator;
    private final LockManager lockManager;
    
    public EngineHolder() throws Exception {
        serviceTaskRegistry = new ServiceTaskRegistryImpl();
        processDefinitionProvider = new TestProcessDefinitionProvider();
        indexedProcessDefinitionProvider = new IndexedProcessDefinitionProvider(processDefinitionProvider);
        eventManager = spy(new EventPersistenceManagerImpl(new InMemEventStorage()));
        persistenceManager = new InMemPersistenceManager();
        expressionManager = new DefaultExpressionManager(serviceTaskRegistry);
        uuidGenerator = new RandomUuidGenerator();
        executor = wrap(new DefaultExecutor(expressionManager, Executors.newCachedThreadPool(), interceptorHolder, indexedProcessDefinitionProvider, uuidGenerator, eventManager, persistenceManager));
        lockManager = new SingleLockManagerImpl();
        
        engine = new AbstractEngine() {
            
            private final Planner planner = new DefaultPlanner();
            
            @Override
            protected UuidGenerator getUuidGenerator() {
                return uuidGenerator;
            }
            
            @Override
            protected IndexedProcessDefinitionProvider getProcessDefinitionProvider() {
                return indexedProcessDefinitionProvider;
            }
            
            @Override
            protected PersistenceManager getPersistenceManager() {
                return persistenceManager;
            }
            
            @Override
            protected ExecutionInterceptorHolder getInterceptorHolder() {
                return interceptorHolder;
            }
            
            @Override
            protected EventPersistenceManager getEventManager() {
                return eventManager;
            }
            
            @Override
            protected LockManager getLockManager() {
                return lockManager;
            }

            @Override
            protected Planner getPlanner() {
                return planner;
            }
            
            @Override
            protected Executor getExecutor() {
                return executor;
            }
        };
        
        activations = new HashMap<>();
        engine.addInterceptor(new Interceptor());
    }
    
    protected Executor wrap(Executor e) {
        return e;
    }
    
    public AbstractEngine getEngine() {
        return engine;
    }
    
    public ServiceTaskRegistryImpl getServiceTaskRegistry() {
        return serviceTaskRegistry;
    }
    
    public EventPersistenceManager getEventManager() {
        return eventManager;
    }
    
    public Executor getExecutor() {
        return executor;
    }
    
    public void deploy(ProcessDefinition pd) {
        processDefinitionProvider.add(pd);
    }
    
    private void onActivation(String businessKey, String processDefinitionId, String elementId) {
        String k = businessKey + "/" + processDefinitionId;
        List<String> l = activations.get(k);
        if (l == null) {
            l = new ArrayList<>();
            activations.put(k, l);
        }

        l.add(elementId);
    }
    
    private void assertActivation(String processBusinessKey, String processDefinitionId, String elementId) {
        String k = processBusinessKey + "/" + processDefinitionId;
        List<String> l = activations.get(k);
        assertNotNull("No activations for " + k, l);
        assertFalse("No more activations for " + k + ", element " + elementId, l.isEmpty());

        String s = l.remove(0);
        assertTrue("Unexpected activation: '" + s + "' instead of '" + elementId + "'", elementId.equals(s));
    }
    
    public void assertActivations(String processBusinessKey, String processDefinitionId, String ... elementIds) {
        for (String eid : elementIds) {
            assertActivation(processBusinessKey, processDefinitionId, eid);
        }
    }
    
    public void assertNoMoreActivations() {
        int s = 0;
        for (List<String> l : activations.values()) {
            s += l.size();
        }
        assertTrue("We have " + s + " more activations", s == 0);
    }
    
    private final class Interceptor extends ExecutionInterceptorAdapter {

        @Override
        public void onElement(ElementEvent ev) throws ExecutionException {
            onActivation(ev.getProcessBusinessKey(), ev.getProcessDefinitionId(), ev.getElementId());
        }
    }
}
