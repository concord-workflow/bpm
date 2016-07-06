package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.ProcessDefinition;
import java.util.HashMap;
import java.util.Map;

public class TestProcessDefinitionProvider implements ProcessDefinitionProvider {

    private final Map<String, ProcessDefinition> defs = new HashMap<>();
    
    @Override
    public ProcessDefinition getById(String id) throws ExecutionException {
        return defs.get(id);
    }
    
    public void add(ProcessDefinition pd) {
        defs.put(pd.getId(), pd);
    }    
}
