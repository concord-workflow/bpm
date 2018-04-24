package io.takari.bpm.reducers;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.EvaluateAndFollowFlowsAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.context.ExecutionContextFactory;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Impure
public class EvaluatedFlowsReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(EvaluatedFlowsReducer.class);

    private final ExecutionContextFactory<?> contextFactory;

    public EvaluatedFlowsReducer(ExecutionContextFactory<?> contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof EvaluateAndFollowFlowsAction)) {
            return state;
        }

        EvaluateAndFollowFlowsAction a = (EvaluateAndFollowFlowsAction) action;

        String nextId = null;

        IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
        List<SequenceFlow> flows = new ArrayList<>(ProcessDefinitionUtils.findOutgoingFlows(pd, a.getElementId()));

        // first, we need to eval all flow expressions and filter out 'false'
        // results

        ExecutionContext ctx = contextFactory.create(state.getVariables(), a.getDefinitionId(), a.getElementId());
        for (Iterator<SequenceFlow> i = flows.iterator(); i.hasNext(); ) {
            SequenceFlow f = i.next();
            if (f.getExpression() == null) {
                continue;
            }

            i.remove();

            if (eval(ctx, f)) {
                nextId = f.getId();
                break;
            }
        }

        // at this point, only flows without expressions are left
        // if we haven't found our flow yet, we need to check a default flow

        if (nextId == null && !flows.isEmpty()) {
            String defaultFlow = a.getDefaultFlow();
            if (defaultFlow != null) {
                // we have the default flow, lets try it
                for (SequenceFlow f : flows) {
                    if (f.getId().equals(defaultFlow)) {
                        nextId = f.getId();
                        break;
                    }
                }
            } else {
                // a default flow is not specified, will take the first one
                nextId = flows.iterator().next().getId();
            }
        }

        if (nextId == null) {
            // no valid flows are found
            throw new ExecutionException("No valid outgoing flows for '%s' and no default flow", a.getElementId());
        }

        CommandStack stack = state.getStack();
        state = state.setStack(stack.push(new ProcessElementCommand(pd.getId(), nextId)));

        state = activateUnusedFlows(state, a.getDefinitionId(), a.getElementId(), nextId);

        return state;
    }

    private static boolean eval(ExecutionContext ctx, SequenceFlow f) {
        String expr = f.getExpression();
        boolean b = ctx.eval(expr, Boolean.class);

        log.debug("eval ['{}', '{}'] -> {}", f.getId(), f.getExpression(), b);
        return b;
    }

    private static ProcessInstance activateUnusedFlows(ProcessInstance state, String definitionId, String elementId, String usedFlowId)
            throws ExecutionException {
        IndexedProcessDefinition pd = state.getDefinition(definitionId);

        for (SequenceFlow f : ProcessDefinitionUtils.findOutgoingFlows(pd, elementId)) {
            if (f.getId().equals(usedFlowId)) {
                continue;
            }
            state = ProcessDefinitionUtils.activateGatewayFlow(state, pd, elementId, -1);
            log.debug("activateUnusedFlows ['{}', '{}', '{}'] -> single activation of '{}'", state.getBusinessKey(), definitionId,
                    elementId, f.getId());
        }
        return state;
    }
}
