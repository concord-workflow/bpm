package io.takari.bpm;

import io.takari.bpm.api.EventService;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.interceptors.ExecutionInterceptorAdapter;
import io.takari.bpm.api.interceptors.InterceptorElementEvent;
import io.takari.bpm.el.DefaultExpressionManager;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.*;
import io.takari.bpm.lock.LockManager;
import io.takari.bpm.lock.SingleLockManagerImpl;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.ProcessDefinitionHelper;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.planner.DefaultPlanner;
import io.takari.bpm.planner.Planner;
import io.takari.bpm.resource.ClassPathResourceResolver;
import io.takari.bpm.resource.ResourceResolver;
import io.takari.bpm.task.ServiceTaskRegistryImpl;
import io.takari.bpm.task.UserTaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;

public class EngineHolder {

    private static final Logger log = LoggerFactory.getLogger(EngineHolder.class);

    private final ServiceTaskRegistryImpl serviceTaskRegistry;
    private final TestProcessDefinitionProvider processDefinitionProvider;
    private final IndexedProcessDefinitionProvider indexedProcessDefinitionProvider;
    private final AbstractEngine engine;
    private final Map<String, List<String>> activations;
    private final EventStorage eventStorage;
    private final EventPersistenceManager eventManager;
    private final PersistenceManager persistenceManager;
    private final Executor executor;
    private final ExpressionManager expressionManager;
    private final ExecutionInterceptorHolder interceptorHolder = new ExecutionInterceptorHolder();
    private final UuidGenerator uuidGenerator;
    private final LockManager lockManager;
    private final Configuration configuration;
    private final DelegatingUserTaskHandler userTaskHandler;
    private final ResourceResolver resourceResolver;

    public EngineHolder() throws Exception {
        serviceTaskRegistry = new ServiceTaskRegistryImpl();
        processDefinitionProvider = spy(new TestProcessDefinitionProvider());
        indexedProcessDefinitionProvider = new IndexedProcessDefinitionProvider(processDefinitionProvider);
        eventStorage = new InMemEventStorage();
        eventManager = spy(new EventPersistenceManagerImpl(eventStorage));
        persistenceManager = new InMemPersistenceManager();
        expressionManager = new DefaultExpressionManager(serviceTaskRegistry);
        uuidGenerator = new TestUuidGenerator();
        configuration = new Configuration();
        lockManager = new SingleLockManagerImpl();
        userTaskHandler = new DelegatingUserTaskHandler();
        resourceResolver = new ClassPathResourceResolver();
        executor = wrap(new DefaultExecutor(configuration, expressionManager, Executors.newCachedThreadPool(),
                interceptorHolder, indexedProcessDefinitionProvider, uuidGenerator, eventManager, persistenceManager,
                userTaskHandler, resourceResolver));

        engine = new AbstractEngine() {

            private final Planner planner = new DefaultPlanner(configuration);
            private final EventService eventService = new EventServiceImpl(lockManager, eventStorage);

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

            @Override
            protected Configuration getConfiguration() {
                return configuration;
            }

            @Override
            public EventService getEventService() {
                return eventService;
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

    public Configuration getConfiguration() {
        return configuration;
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

    public UuidGenerator getUuidGenerator() {
        return uuidGenerator;
    }

    public ProcessDefinitionProvider getProcessDefinitionProvider() {
        return processDefinitionProvider;
    }

    public DelegatingUserTaskHandler getUserTaskHandler() {
        return userTaskHandler;
    }

    public void deploy(ProcessDefinition pd) {
        if (log.isDebugEnabled()) {
            log.debug("deploy ->\n{}", ProcessDefinitionHelper.dump(pd));
        }
        processDefinitionProvider.add(pd);
    }

    public void deploy(Map<String, ProcessDefinition> pds) {
        for (ProcessDefinition p : pds.values()) {
            if (log.isDebugEnabled()) {
                log.debug("deploy ->\n{}", ProcessDefinitionHelper.dump(p));
            }
            processDefinitionProvider.add(p);
        }
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

    public void assertActivations(String processBusinessKey, String processDefinitionId, String... elementIds) {
        for (String eid : elementIds) {
            assertActivation(processBusinessKey, processDefinitionId, eid);
        }
    }

    public void assertNoMoreActivations() {
        int s = 0;
        for (List<String> l : activations.values()) {
            s += l.size();
        }
        assertTrue("We have " + s + " more activations: " + activations, s == 0);
    }

    public void dumpActivations() {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, List<String>> e : activations.entrySet()) {
            b.append(e.getKey()).append(":\n");
            for (String a : e.getValue()) {
                b.append("\t").append(a).append("\n");
            }
        }
        log.debug("dumpActivations ->\n{}", b);
    }

    private final class Interceptor extends ExecutionInterceptorAdapter {

        @Override
        public void onElement(InterceptorElementEvent ev) throws ExecutionException {
            onActivation(ev.getProcessBusinessKey(), ev.getProcessDefinitionId(), ev.getElementId());
        }
    }
}
