package io.takari.bpm.reducers;

import java.util.Map;

import io.takari.bpm.ExecutionInterceptorHolder;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FireOnElementInterceptorsAction;
import io.takari.bpm.actions.FireOnErrorInterceptorsAction;
import io.takari.bpm.actions.FireOnFinishInterceptorsAction;
import io.takari.bpm.actions.FireOnResumeInterceptorsAction;
import io.takari.bpm.actions.FireOnStartInterceptorsAction;
import io.takari.bpm.actions.FireOnSuspendInterceptorsAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Variables;

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
        }

        return state;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<String, Object> getCurrentVariables(ProcessInstance state) {
        // this is stupid, but this is how variables are exposed in API
        Variables vars = state.getVariables();
        Map m = vars.asMap();
        return (Map<String, Object>) m;
    }
}
