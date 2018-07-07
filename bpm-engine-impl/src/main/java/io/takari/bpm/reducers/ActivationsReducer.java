package io.takari.bpm.reducers;

import io.takari.bpm.ExecutionInterceptorHolder;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ActivateElementAction;
import io.takari.bpm.actions.ActivateFlowsAction;
import io.takari.bpm.actions.ActivateFlowsAction.Flow;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.state.Activations;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Impure
public class ActivationsReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(ActivationsReducer.class);

    private final ExecutionInterceptorHolder interceptors;

    public ActivationsReducer(ExecutionInterceptorHolder interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        Scopes scopes = state.getScopes();
        UUID scopeId = scopes.getCurrentId();

        if (action instanceof ActivateFlowsAction) {

            ActivateFlowsAction a = (ActivateFlowsAction) action;
            IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
            
            for (Flow flow : a.getFlows()) {
                state = ProcessDefinitionUtils.activateGatewayFlow(state, pd, flow.getElementId(), flow.getCount());
            }

            return state;

        } else if (action instanceof ActivateElementAction) {

            ActivateElementAction a = (ActivateElementAction) action;
            IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());

            AbstractElement ev = ProcessDefinitionUtils.findElement(pd, a.getElementId());

            if (ev instanceof StartEvent) {
                state = ProcessDefinitionUtils.activateGatewayFlow(state, pd, a.getElementId(), a.getCount());
            }

            if (ev instanceof SequenceFlow) {
                Activations acts = state.getActivations();
                SequenceFlow sf = (SequenceFlow) ev;
                AbstractElement target = ProcessDefinitionUtils.findElement(pd, sf.getTo());
                if (ProcessDefinitionUtils.isParallelGateway(target)) {
                    acts = acts.inc(scopes, scopeId, a.getElementId(), a.getCount());
                }
                state = state.setActivations(acts);
            }

            interceptors.fireOnElement(state.getVariables(), state.getBusinessKey(), a.getDefinitionId(), state.getId(), scopeId, a.getElementId());

            log.debug("reduce ['{}', '{}', '{}'] -> single activation", state.getBusinessKey(), a.getElementId(), a.getCount());
            return state;

        }
        return state;
    }

}
