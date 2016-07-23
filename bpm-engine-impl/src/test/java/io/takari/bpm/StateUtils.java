package io.takari.bpm;

import java.util.UUID;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.state.ProcessInstance;

public final class StateUtils {

    public static ProcessInstance empty(ProcessDefinition pd) {
        return new ProcessInstance(UUID.randomUUID(), UUID.randomUUID().toString(), new IndexedProcessDefinition(pd));
    }
    
    private StateUtils() {
    }
}
