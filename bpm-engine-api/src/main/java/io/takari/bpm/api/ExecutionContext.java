package io.takari.bpm.api;

import java.util.Map;
import java.util.Set;

/**
 * The execution context. Provides access to the process variables.
 */
public interface ExecutionContext {
    
    /**
     * Key of latest handled error, contains "errorRef" of a boundary error
     * event. Can be accessed with
     * {@link #getVariable(ExecutionContext.ERROR_CODE_KEY)}.
     */
    String ERROR_CODE_KEY = "errorCode";
    
    /**
     * Key of latest handled error's cause, contains the cause, if any, of a boundary error
     * event. Can be accessed with
     * {@link #getVariable(ExecutionContext.ERROR_CAUSE_KEY)}.
     */
    String ERROR_CAUSE_KEY = "errorCause";

    String PROCESS_BUSINESS_KEY = "__processBusinessKey";

    Object getVariable(String key);
    
    Map<String, Object> getVariables();

    void setVariable(String key, Object value);
    
    boolean hasVariable(String key);
    
    void removeVariable(String key);

    Set<String> getVariableNames();

    <T> T eval(String expr, Class<T> type);
}
