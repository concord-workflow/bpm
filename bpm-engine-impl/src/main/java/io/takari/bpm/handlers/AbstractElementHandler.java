package io.takari.bpm.handlers;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.IndexedProcessDefinitionProvider;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;

public abstract class AbstractElementHandler implements ElementHandler {

    private final AbstractEngine engine;

    public AbstractElementHandler(AbstractEngine engine) {
        this.engine = engine;
    }

    protected AbstractEngine getEngine() {
        return engine;
    }

    protected IndexedProcessDefinition getProcessDefinition(ProcessElementCommand c) throws ExecutionException {
        return getProcessDefinition(c.getProcessDefinitionId());
    }

    protected IndexedProcessDefinition getProcessDefinition(String processDefinitionId) throws ExecutionException {
        IndexedProcessDefinitionProvider provider = engine.getProcessDefinitionProvider();
        return provider.getById(processDefinitionId);
    }
}
