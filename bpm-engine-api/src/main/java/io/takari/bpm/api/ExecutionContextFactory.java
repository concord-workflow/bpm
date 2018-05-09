package io.takari.bpm.api;

import java.util.Map;

public interface ExecutionContextFactory<T extends ExecutionContext> {

    T create(Variables source);

    T create(Variables source, String processDefinitionId, String elementId);

    ExecutionContext withOverrides(ExecutionContext delegate, Map<Object, Object> overrides);
}
