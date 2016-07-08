package io.takari.bpm;

import io.takari.bpm.api.Engine;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.api.interceptors.ElementEvent;
import io.takari.bpm.api.interceptors.ExecutionInterceptorAdapter;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.task.ServiceTaskRegistryImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Before;
import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEngineTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractEngineTest.class);

    private TestProcessDefinitionProvider processDefinitionProvider;
    private ServiceTaskRegistryImpl serviceTaskRegistry;
    protected EventPersistenceManager eventManager;
    private Engine engine;
    private Map<String, List<String>> activations;

    @Before
    public void init() {
        processDefinitionProvider = new TestProcessDefinitionProvider();
        serviceTaskRegistry = new ServiceTaskRegistryImpl();
        eventManager = spy(new EventPersistenceManagerImpl(new InMemEventStorage()));

        engine = new EngineBuilder()
                .withDefinitionProvider(processDefinitionProvider)
                .withEventManager(eventManager)
                .withTaskRegistry(serviceTaskRegistry)
                .build();

        activations = new HashMap<>();
        engine.addInterceptor(new Interceptor());
    }

    protected ServiceTaskRegistryImpl getServiceTaskRegistry() {
        return serviceTaskRegistry;
    }
    
    protected void deploy(ProcessDefinition pd) {
        IndexedProcessDefinition ipd = new IndexedProcessDefinition(pd);
        processDefinitionProvider.add(ipd);
    }

    protected Engine getEngine() {
        return engine;
    }

    public EventPersistenceManager getEventManager() {
        return eventManager;
    }
    
    protected void register(String key, JavaDelegate d) {
        serviceTaskRegistry.register(key, d);
    }

    public void onActivation(String businessKey, String processDefinitionId, String elementId) {
        String k = businessKey + "/" + processDefinitionId;
        List<String> l = activations.get(k);
        if (l == null) {
            l = new ArrayList<>();
            activations.put(k, l);
        }

        l.add(elementId);
    }

    protected void assertActivation(String processBusinessKey, String processDefinitionId, String elementId) {
        String k = processBusinessKey + "/" + processDefinitionId;
        List<String> l = activations.get(k);
        assertNotNull("No activations for " + k, l);
        assertFalse("No more activations for " + k + ", element " + elementId, l.isEmpty());

        String s = l.remove(0);
        assertTrue("Unexpected activation: '" + s + "' instead of '" + elementId + "'", elementId.equals(s));
    }

    protected void assertActivations(String processBusinessKey, String processDefinitionId, String ... elementIds) {
        for (String eid : elementIds) {
            assertActivation(processBusinessKey, processDefinitionId, eid);
        }
    }

    protected void assertNoMoreActivations() {
        StringBuilder b = new StringBuilder();
        int s = 0;
        for (List<String> l : activations.values()) {
            s += l.size();
        }
        assertTrue("We have " + s + " more activations", s == 0);
    }

    protected void dumpActivations(String processBusinessKey, String processDefinitionId) {
        String k = processBusinessKey + "/" + processDefinitionId;
        List<String> l = activations.get(k);
        log.info("dumpActivations ['{}', '{}'] -> done: {}", processBusinessKey, processDefinitionId,
                Arrays.asList(l.toArray(new String[l.size()])));
    }
    
    private final class Interceptor extends ExecutionInterceptorAdapter {

        @Override
        public void onElement(ElementEvent ev) throws ExecutionException {
            onActivation(ev.getProcessBusinessKey(), ev.getProcessDefinitionId(), ev.getElementId());
        }
    }
}
