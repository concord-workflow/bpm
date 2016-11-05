package io.takari.bpm.reducers;

import io.takari.bpm.ExecutionInterceptorHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ActivateElementAction;
import io.takari.bpm.actions.ActivateFlowsAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.state.Activations;
import io.takari.bpm.state.ProcessInstance;

@Impure
public class ActivationsReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(ActivationsReducer.class);

    private final ExecutionInterceptorHolder interceptors;

    public ActivationsReducer(ExecutionInterceptorHolder interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (action instanceof ActivateFlowsAction) {
            ActivateFlowsAction a = (ActivateFlowsAction) action;

            Activations acts = state.getActivations();
            for (String elementId : a.getElementIds()) {
                acts = activateFlows(state, acts, a.getDefinitionId(), elementId, a.getCount());
            }

            return state.setActivations(acts);
        } else if (action instanceof ActivateElementAction) {
            ActivateElementAction a = (ActivateElementAction) action;
            Activations acts = state.getActivations();
            acts = acts.inc(a.getDefinitionId(), a.getElementId(), a.getCount());

            interceptors.fireOnElement(state.getBusinessKey(), a.getDefinitionId(), state.getId(), a.getElementId());

            log.debug("reduce ['{}', '{}', '{}'] -> single activation", state.getBusinessKey(), a.getElementId(), a.getCount());
            return state.setActivations(acts);
        }

        return state;
    }

    private static Activations activateFlows(ProcessInstance state, Activations acts, String definitionId, String elementId, int count)
            throws ExecutionException {

        IndexedProcessDefinition pd = state.getDefinition(definitionId);
        String gwId = ProcessDefinitionUtils.findNextGatewayId(pd, elementId);
        if (gwId == null) {
            return acts;
        }

        log.debug("activateFlows ['{}', '{}'] -> activating '{}' via '{}' (count: {})", state.getBusinessKey(), elementId, gwId, elementId, count);
        acts = acts.inc(pd.getId(), gwId, count);

        return acts;
    }
}
