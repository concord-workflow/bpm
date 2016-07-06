package io.takari.bpm.api.interceptors;

import io.takari.bpm.api.ExecutionException;

/**
 * Work in progress.
 */
public interface ExecutionInterceptor {
    
    void onStart(InterceptorStartEvent ev) throws ExecutionException;
    
    void onSuspend() throws ExecutionException;
    
    void onResume() throws ExecutionException;
    
    void onFinish(String processBusinessKey) throws ExecutionException;
    
    void onElement(ElementEvent ev) throws ExecutionException;
    
    void onError(String processBusinessKey, Throwable cause) throws ExecutionException;
}
