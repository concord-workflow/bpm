package io.takari.bpm.api;

import java.util.Map;
import java.util.Set;

/**
 * The execution context. Provides access to the process variables.
 */
public interface ExecutionContext {
    
    /**
     * Key of latest handled @{BpmnError}. Can be accessed with @{code #getVariable(LAST_ERROR_KEY}.
     */
    String LAST_ERROR_KEY = "lastError";

    String PROCESS_BUSINESS_KEY = "__processBusinessKey";

    String CURRENT_FLOW_NAME_KEY = "__currentFlow";

    Object getVariable(String key);
    
    Map<String, Object> getVariables();

    void setVariable(String key, Object value);
    
    boolean hasVariable(String key);
    
    void removeVariable(String key);

    Set<String> getVariableNames();

    <T> T eval(String expr, Class<T> type);

    Map<String, Object> toMap();

    Object interpolate(Object v);

    void suspend(String messageRef);

    void suspend(String messageRef, Object payload, boolean resumeFromSameStep);

    String getProcessDefinitionId();

    String getElementId();
}
