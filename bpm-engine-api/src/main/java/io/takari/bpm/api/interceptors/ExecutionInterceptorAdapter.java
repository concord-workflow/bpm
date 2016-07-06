package io.takari.bpm.api.interceptors;

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
    public void onElement(ElementEvent ev) throws ExecutionException {
    }

    @Override
    public void onError(String processBusinessKey, Throwable cause) throws ExecutionException {
    }
}
