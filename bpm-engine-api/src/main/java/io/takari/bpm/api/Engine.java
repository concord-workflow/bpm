package io.takari.bpm.api;

import io.takari.bpm.api.interceptors.ExecutionInterceptor;

import java.util.Map;
import java.util.UUID;

public interface Engine {

    /**
     * Starts a new process instance with the given ID.
     * @param processBusinessKey an external process instance ID, must be unique per {@link Engine}.
     * @param processDefinitionId the id of the process definition, can't be null.
     * @param arguments arguments to be passed, can be null.
     * @throws ExecutionException
     */
    void start(String processBusinessKey, String processDefinitionId, Map<String, Object> arguments) throws ExecutionException;

    /**
     * Starts a new process instance with the given ID.
     * @param processBusinessKey an external process instance ID, must be unique per {@link Engine}.
     * @param processDefinitionId the id of the process definition, can't be null.
     * @param variables initial variables
     * @param arguments extra arguments, can be null.
     * @throws ExecutionException
     */
    void start(String processBusinessKey, String processDefinitionId, Variables variables, Map<String, Object> arguments) throws ExecutionException;

    /**
     * Resumes an process instance, waiting for the specific event.
     * @param processBusinessKey an external process instance ID.
     * @param eventName the name of the event, can't be null.
     * @param variables variables to be passed, can be null. Values with the same
     * variable names will be replaced.
     * @throws ExecutionException
     */
    void resume(String processBusinessKey, String eventName, Map<String, Object> variables) throws ExecutionException;

    /**
     * @see #resume(String, String, Map)
     * @param merge if {@code true}, then the variables will be merged into existing
     * process values.
     * @throws ExecutionException
     */
    void resume(String processBusinessKey, String eventName, Map<String, Object> variables, boolean merge) throws ExecutionException;

    /**
     * Resumes a process instance, waiting for the specific event.
     * @param eventId the ID of event.
     * @param variables variables to be passed, can be null. Values with the same
     * variable names will be replaced.
     * @throws ExecutionException
     */
    void resume(UUID eventId, Map<String, Object> variables) throws ExecutionException;


    /**
     * @see #resume(UUID, Map)
     * @param merge if {@code true}, then the variables will be merged into existing
     * process values.
     * @throws ExecutionException
     */
    void resume(UUID eventId, Map<String, Object> variables, boolean merge) throws ExecutionException;

    /**
     * Adds an execution interceptor. Execution interceptor will receive events
     * synchronously and may block the execution. It is recommended to configure
     * all interceptors before the first execution.
     * @param i
     */
    void addInterceptor(ExecutionInterceptor i);

    /**
     * Provides utility methods to work with BPM events.
     * @return instance of the event manager.
     */
    EventService getEventService();
}
