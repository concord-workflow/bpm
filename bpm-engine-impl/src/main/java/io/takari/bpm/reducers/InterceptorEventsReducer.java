package io.takari.bpm.reducers;

import io.takari.bpm.ExecutionInterceptorHolder;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;

import java.util.Map;
import java.util.UUID;

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
            UUID scopeId = state.getScopes().getCurrentId();
            interceptors.fireOnElement(state.getVariables(), state.getBusinessKey(), a.getDefinitionId(), state.getId(), scopeId, a.getElementId());
        } else if (action instanceof FireOnStartInterceptorsAction) {
            FireOnStartInterceptorsAction a = (FireOnStartInterceptorsAction) action;
            interceptors.fireOnStart(state.getBusinessKey(), a.getDefinitionId(), state.getId(), getCurrentVariables(state));
        } else if (action instanceof FireOnSuspendInterceptorsAction) {
            interceptors.fireOnSuspend();
        } else if (action instanceof FireOnResumeInterceptorsAction) {
            interceptors.fireOnResume();
        } else if (action instanceof FireOnFinishInterceptorsAction) {
            interceptors.fireOnFinish(state.getBusinessKey());
        } else if (action instanceof FireOnFailureInterceptorsAction) {
            FireOnFailureInterceptorsAction a = (FireOnFailureInterceptorsAction) action;
            interceptors.fireOnFailure(state.getBusinessKey(), a.getErrorRef());
        } else if (action instanceof FireOnUnhandledErrorAction) {
            FireOnUnhandledErrorAction a = (FireOnUnhandledErrorAction) action;
            interceptors.fireOnUnhandledError(state.getBusinessKey(), a.getError());
        } else if (action instanceof FireOnScopeCreatedInterceptorsAction) {
            FireOnScopeCreatedInterceptorsAction a = (FireOnScopeCreatedInterceptorsAction) action;
            interceptors.fireOnScopeCreated(state.getBusinessKey(), a.getDefinitionId(), state.getId(), a.getScopeId(), a.getElementId());
        } else if (action instanceof FireOnScopeDestroyedInterceptorsAction) {
            FireOnScopeDestroyedInterceptorsAction a = (FireOnScopeDestroyedInterceptorsAction) action;
            interceptors.fireOnScopeDestroyed(state.getBusinessKey(), state.getId(), a.getScopeId());
        }

        return state;
    }

    private static Map<String, Object> getCurrentVariables(ProcessInstance state) {
        return state.getVariables().asMap();
    }
}
