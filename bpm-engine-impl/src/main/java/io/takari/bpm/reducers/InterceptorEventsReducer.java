package io.takari.bpm.reducers;

import java.util.Map;

import io.takari.bpm.ExecutionInterceptorHolder;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

@Impure
public class InterceptorEventsReducer implements Reducer {

    private final ExecutionInterceptorHolder interceptors;

    public InterceptorEventsReducer(ExecutionInterceptorHolder interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (action instanceof FireOnElementInterceptorsAction) {
            FireOnElementInterceptorsAction a = (FireOnElementInterceptorsAction) action;
            interceptors.fireOnElement(state.getBusinessKey(), a.getDefinitionId(), state.getId(), a.getElementId());
        } else if (action instanceof FireOnStartInterceptorsAction) {
            FireOnStartInterceptorsAction a = (FireOnStartInterceptorsAction) action;
            interceptors.fireOnStart(state.getBusinessKey(), a.getDefinitionId(), state.getId(), getCurrentVariables(state));
        } else if (action instanceof FireOnSuspendInterceptorsAction) {
            interceptors.fireOnSuspend();
        } else if (action instanceof FireOnResumeInterceptorsAction) {
            interceptors.fireOnResume();
        } else if (action instanceof FireOnFinishInterceptorsAction) {
            interceptors.fireOnFinish(state.getBusinessKey());
        } else if (action instanceof FireOnErrorInterceptorsAction) {
            FireOnErrorInterceptorsAction a = (FireOnErrorInterceptorsAction) action;
            interceptors.fireOnError(state.getBusinessKey(), a.getCause());
        } else if (action instanceof FireOnFailureInterceptorsAction) {
            FireOnFailureInterceptorsAction a = (FireOnFailureInterceptorsAction) action;
            interceptors.fireOnFailure(state.getBusinessKey(), a.getErrorRef());
        } else if (action instanceof FireOnUnhandledErrorAction) {
            FireOnUnhandledErrorAction a = (FireOnUnhandledErrorAction) action;
            interceptors.fireOnUnhandledError(state.getBusinessKey(), a.getError());
        }

        return state;
    }

    private static Map<String, Object> getCurrentVariables(ProcessInstance state) {
        return state.getVariables().asMap();
    }
}
