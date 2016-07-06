package io.takari.bpm.testkit;

import io.takari.bpm.ProcessDefinitionProvider;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.ProcessDefinition;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestProcessDefinitionProvider implements ProcessDefinitionProvider {

    private final Map<String, ProcessDefinition> m = new ConcurrentHashMap<>();

    public void add(ProcessDefinition d) {
        m.put(d.getId(), d);
    }

    @Override
    public ProcessDefinition getById(String id) throws ExecutionException {
        return m.get(id);
    }
}
