package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexedProcessDefinitionProvider {

    private static final Logger log = LoggerFactory.getLogger(IndexedProcessDefinitionProvider.class);

    private final ProcessDefinitionProvider delegate;

    public IndexedProcessDefinitionProvider(final ProcessDefinitionProvider delegate) {
        this.delegate = delegate;
    }

    public IndexedProcessDefinition getById(final String id) throws ExecutionException {
        ProcessDefinition pd = delegate.getById(id);
        if (pd == null) {
            throw new ExecutionException("Process '%s' not found", id);
        }

        IndexedProcessDefinition ipd = new IndexedProcessDefinition(pd);
        log.info("getById ['{}'] -> indexing done", id);
        return ipd;
    }
}
