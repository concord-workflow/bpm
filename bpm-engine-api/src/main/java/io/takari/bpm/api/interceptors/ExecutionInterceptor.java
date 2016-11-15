package io.takari.bpm.api.interceptors;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionException;

/**
 * Work in progress.
 */
public interface ExecutionInterceptor {
    
    void onStart(InterceptorStartEvent ev) throws ExecutionException;
    
    void onSuspend() throws ExecutionException;
    
    void onResume() throws ExecutionException;
    
    void onFinish(String processBusinessKey) throws ExecutionException;

    void onFailure(String processBusinessKey, String errorRef) throws ExecutionException;

    void onUnhandledError(String processBusinessKey, BpmnError bpmnError) throws ExecutionException;
    
    void onElement(InterceptorElementEvent ev) throws ExecutionException;

    @Deprecated
    void onError(String processBusinessKey, Throwable cause) throws ExecutionException;

    void onError(InterceptorErrorEvent ev) throws ExecutionException;

    void onScopeCreated(InterceptorScopeCreatedEvent ev) throws ExecutionException;

    void onScopeDestroyed(InterceptorScopeDestroyedEvent ev) throws ExecutionException;
}
