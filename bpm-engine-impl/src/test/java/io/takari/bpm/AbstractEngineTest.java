package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.Engine;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.task.ServiceTaskRegistryImpl;
import org.junit.Before;

import java.util.Map;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class AbstractEngineTest {

    private EngineHolder engineHolder;

    @Before
    public void init() throws Exception {
        this.engineHolder = new EngineHolder();
    }

    protected UUID randomUuid() {
        return engineHolder.getUuidGenerator().generate();
    }

    protected Configuration getConfiguration() {
        return engineHolder.getConfiguration();
    }

    protected Engine getEngine() {
        return engineHolder.getEngine();
    }

    protected ServiceTaskRegistryImpl getServiceTaskRegistry() {
        return engineHolder.getServiceTaskRegistry();
    }

    protected EventPersistenceManager getEventManager() {
        return engineHolder.getEventManager();
    }

    protected ProcessDefinitionProvider getProcessDefinitionProvider() {
        return engineHolder.getProcessDefinitionProvider();
    }

    protected DelegatingUserTaskHandler getUserTaskHandler() {
        return engineHolder.getUserTaskHandler();
    }

    protected UuidGenerator getUuidGenerator() {
        return engineHolder.getUuidGenerator();
    }

    protected void deploy(ProcessDefinition pd) {
        engineHolder.deploy(pd);
    }

    protected void deploy(Map<String, ProcessDefinition> pds) {
        engineHolder.deploy(pds);
    }

    protected void assertActivations(String processBusinessKey, String processDefinitionId, String... elementIds) {
        engineHolder.assertActivations(processBusinessKey, processDefinitionId, elementIds);
    }

    protected void dumpActivations() {
        engineHolder.dumpActivations();
    }

    protected void assertNoMoreActivations() {
        engineHolder.assertNoMoreActivations();
    }

    protected static void assertBpmnError(Exception e, String errorRef) {
        BpmnError err = null;
        if (e instanceof BpmnError) {
            err = (BpmnError) e;
        } else if (e.getCause() instanceof BpmnError) {
            err = (BpmnError) e.getCause();
        }

        assertNotNull(err);
        assertEquals(errorRef, err.getErrorRef());
    }
}
