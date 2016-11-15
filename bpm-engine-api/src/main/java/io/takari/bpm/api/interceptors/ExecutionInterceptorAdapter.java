package io.takari.bpm.api.interceptors;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionException;

public abstract class ExecutionInterceptorAdapter implements ExecutionInterceptor {

    @Override
    public void onStart(InterceptorStartEvent ev) throws ExecutionException {
    }

    @Override
    public void onSuspend() throws ExecutionException {
    }

    @Override
    public void onResume() throws ExecutionException {
    }

    @Override
    public void onFinish(String processBusinessKey) throws ExecutionException {
    }

    @Override
    public void onFailure(String processBusinessKey, String errorRef) throws ExecutionException {
    }

    @Override
    public void onUnhandledError(String processBusinessKey, BpmnError bpmnError) throws ExecutionException {
    }

    @Override
    public void onElement(InterceptorElementEvent ev) throws ExecutionException {
    }

    @Override
    public void onError(String processBusinessKey, Throwable cause) throws ExecutionException {
    }

    @Override
    public void onError(InterceptorErrorEvent ev) throws ExecutionException {
    }

    @Override
    public void onScopeCreated(InterceptorScopeCreatedEvent ev) throws ExecutionException {
    }

    @Override
    public void onScopeDestroyed(InterceptorScopeDestroyedEvent ev) throws ExecutionException {
    }
}
