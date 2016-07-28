package io.takari.bpm.reducers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FollowNewGroupAction;
import io.takari.bpm.actions.InclusiveForkAction;
import io.takari.bpm.actions.ParallelForkAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.Activations;
import io.takari.bpm.state.ProcessInstance;

@Impure
public class ForkReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(ForkReducer.class);

    private final ExpressionManager expressionManager;
    
    public ForkReducer(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (action instanceof ParallelForkAction) {
            ParallelForkAction a = (ParallelForkAction) action;

            // parallel gateway doesn't evaluate the expressions
            IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
            List<SequenceFlow> out = ProcessDefinitionUtils.findOutgoingFlows(pd, a.getElementId());

            return follow(state, a.getDefinitionId(), a.getElementId(), true, out);
        } else if (action instanceof InclusiveForkAction) {
            InclusiveForkAction a = (InclusiveForkAction) action;

            // inclusive gateway should evaluate flow expression and filter out
            // those, which was evaluated into 'false'
            IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
            List<SequenceFlow> out = ProcessDefinitionUtils.findOutgoingFlows(pd, a.getElementId());
            
            ExecutionContextImpl ctx = new ExecutionContextImpl(expressionManager, state.getVariables());
            List<SequenceFlow> filtered = filterInactive(expressionManager, ctx, out);

            // TODO refactor into an utility fn?
            if (!ctx.toActions().isEmpty()) {
                log.warn("reduce ['{}', '{}'] -> variables changes in the execution context will be ignored", state.getBusinessKey(),
                        a.getElementId());
            }

            List<SequenceFlow> inactive = new ArrayList<>(out);
            inactive.removeAll(filtered);
            if (!inactive.isEmpty()) {
                String gwId = ProcessDefinitionUtils.findNextGatewayId(pd, a.getElementId());
                int count = inactive.size();

                // directly activate the unused flows, so they will be visible
                // for the next commands on the stack
                Activations acts = state.getActivations();
                state = state.setActivations(acts.inc(pd.getId(), gwId, count));
            }

            return follow(state, a.getDefinitionId(), a.getElementId(), false, filtered);
        }

        return state;
    }

    private static ProcessInstance follow(ProcessInstance state, String definitionId, String elementId, boolean exclusive, List<SequenceFlow> flows) {
        CommandStack stack = state.getStack()
                .push(new PerformActionsCommand(
                        new FollowNewGroupAction(definitionId, elementId, false, ProcessDefinitionUtils.toIds(flows))));

        return state.setStack(stack);
    }
    
    private static List<SequenceFlow> filterInactive(ExpressionManager em, ExecutionContext ctx, List<SequenceFlow> flows) {
        List<SequenceFlow> result = new ArrayList<SequenceFlow>(flows);

        for (Iterator<SequenceFlow> i = result.iterator(); i.hasNext();) {
            SequenceFlow f = i.next();
            if (f.getExpression() != null) {
                String expr = f.getExpression();
                boolean b = em.eval(ctx, expr, Boolean.class);
                if (!b) {
                    i.remove();
                }
            }
        }

        return result;
    }
}
