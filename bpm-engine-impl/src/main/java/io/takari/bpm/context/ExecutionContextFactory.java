package io.takari.bpm.context;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.state.Variables;

public interface ExecutionContextFactory<T extends ExecutionContext> {

    T create(Variables source);

    T create(Variables source, String processDefinitionId, String elementId);
}
