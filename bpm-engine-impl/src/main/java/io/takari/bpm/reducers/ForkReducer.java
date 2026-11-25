package io.takari.bpm.reducers;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.Activations;
import io.takari.bpm.state.Forks;
import io.takari.bpm.state.Forks.Fork;
import io.takari.bpm.state.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Impure
public class ForkReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(ForkReducer.class);

    private final ExecutionContextFactory<?> contextFactory;

    public ForkReducer(ExecutionContextFactory<?> contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (action instanceof ParallelForkAction) {
            ParallelForkAction a = (ParallelForkAction) action;

            IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
            if (!ProcessDefinitionUtils.isJoinComplete(state, pd, a.getElementId(), false)) {
                // the join was already activated (e.g. re-evaluated after one of the tokens took another path)
                log.debug("reduce ['{}', '{}'] -> join is not complete, skipping", state.getBusinessKey(), a.getElementId());
                return state;
            }
            state = consumeJoin(state, pd, a.getElementId());

            // parallel gateway doesn't evaluate the expressions
            // (the list is modified by #follow, the definition's list is immutable)
            List<SequenceFlow> out = new ArrayList<>(ProcessDefinitionUtils.findOutgoingFlows(pd, a.getElementId()));

            return follow(pd, state, a.getDefinitionId(), a.getElementId(), out, null);
        } else if (action instanceof InclusiveForkAction) {
            InclusiveForkAction a = (InclusiveForkAction) action;

            IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
            if (!ProcessDefinitionUtils.isJoinComplete(state, pd, a.getElementId(), false)) {
                log.debug("reduce ['{}', '{}'] -> join is not complete, skipping", state.getBusinessKey(), a.getElementId());
                return state;
            }
            state = consumeJoin(state, pd, a.getElementId());

            // inclusive gateway should evaluate flow expression and filter out
            // those, which was evaluated into 'false'
            List<SequenceFlow> out = ProcessDefinitionUtils.findOutgoingFlows(pd, a.getElementId());

            ExecutionContext ctx = contextFactory.create(state.getVariables(), a.getDefinitionId(), a.getElementId());
            List<SequenceFlow> filtered = filterInactive(ctx, out);

            List<SequenceFlow> inactive = new ArrayList<>(out);
            inactive.removeAll(filtered);

            return follow(pd, state, a.getDefinitionId(), a.getElementId(), filtered, inactive);
        } else if (action instanceof CommenceForkAction) {
            CommenceForkAction a = (CommenceForkAction) action;

            Forks forks = state.getForks();
            UUID scopeId = state.getScopes().getCurrentId();
            Fork fork = forks.getFork(scopeId, a.getElementId());

            ActivateFlowsAction activateFlows = ActivateFlowsAction.empty(a.getDefinitionId());

            List<String> flows = new ArrayList<>();
            List<Action> actions = new ArrayList<>();
            for (String flowId : fork.getFlows()) {
                int count = fork.getFlowCount(flowId);
                activateFlows = activateFlows.addFlow(flowId, count);

                // follow the flow the required number of times
                for (int i = 0; i < count; i++) {
                    flows.add(flowId);
                }
            }
            actions.add(activateFlows);
            actions.add(new FollowFlowsAction(a.getDefinitionId(), a.getElementId(), flows));

            CommandStack stack = state.getStack()
                    .push(new PerformActionsCommand(new PopScopeAction()))
                    .push(new PerformActionsCommand(actions))
                    .push(new PerformActionsCommand(new PushScopeAction(a.getDefinitionId(), a.getElementId(), false)));

            return state.setForks(forks.removeFork(scopeId, a.getElementId()))
                    .setStack(stack);
        }

        return state;
    }

    /**
     * Removes the join's activations: the tokens are consumed by the gateway.
     * The next activation of the same gateway (e.g. in a loop) starts from scratch.
     */
    private static ProcessInstance consumeJoin(ProcessInstance state, IndexedProcessDefinition pd, String elementId) throws ExecutionException {
        List<SequenceFlow> in = ProcessDefinitionUtils.findIncomingFlows(pd, elementId);
        Activations acts = state.getActivations()
                .consume(state.getScopes(), state.getScopes().getCurrentId(), ProcessDefinitionUtils.toIds(in));
        return state.setActivations(acts);
    }

    private static ProcessInstance follow(IndexedProcessDefinition pd, ProcessInstance state, String definitionId, String elementId, List<SequenceFlow> flows, List<SequenceFlow> inactiveFlows) throws ExecutionException {

        CommandStack stack = state.getStack();
        Forks forks = state.getForks();
        UUID scopeId = state.getScopes().getCurrentId();

        if (!state.getForks().containsFork(scopeId, elementId)) {
            stack = stack.push(new PerformActionsCommand(new CommenceForkAction(definitionId, elementId)));
        }

        // special case for 'self-agitating-parallel-loop'
        // if the first flow is a loop to itself, send it separately in the current scope and accumulate the resulting
        // activation counts before commencing
        if (flows.size() > 0) {
            SequenceFlow ff = flows.get(0);
            if (isSelfLoop(pd, ff, elementId)) {
                flows.remove(0);
                stack = stack.push(new PerformActionsCommand(Arrays.asList(
                        new ActivateFlowsAction(definitionId, ff.getId(), 1),
                        new FollowFlowsAction(definitionId, elementId, Collections.singletonList(ff.getId()))
                )));
            }
        }

        // increment expected gateway activations downstream
        for (SequenceFlow flow : flows) {
            forks = forks.incrementFlow(scopeId, elementId, flow.getId(), 1);
        }

        // and for inactive flows
        if (inactiveFlows != null) {
            for (SequenceFlow flow : inactiveFlows) {
                forks = forks.incrementFlow(scopeId, elementId, flow.getId(), 0);
            }
        }

        return state.setForks(forks).setStack(stack);
    }

    /**
     * @return {@code true} if a token sent into the flow returns directly to the gateway, i.e. without
     * passing through any other join.
     */
    private static boolean isSelfLoop(IndexedProcessDefinition pd, SequenceFlow flow, String gatewayId) throws ExecutionException {
        for (String flowId : ProcessDefinitionUtils.findReachableJoinFlows(pd, flow.getId()).keySet()) {
            SequenceFlow f = (SequenceFlow) ProcessDefinitionUtils.findElement(pd, flowId);
            if (gatewayId.equals(f.getTo())) {
                return true;
            }
        }
        return false;
    }

    private static List<SequenceFlow> filterInactive(ExecutionContext ctx, List<SequenceFlow> flows) throws ExecutionException {
        List<SequenceFlow> result = new ArrayList<>(flows);

        for (Iterator<SequenceFlow> i = result.iterator(); i.hasNext(); ) {
            SequenceFlow f = i.next();
            if (f.getExpression() != null) {
                String expr = f.getExpression();
                Boolean b = ctx.eval(expr, Boolean.class);
                if (b == null) {
                    throw new ExecutionException("'{}' expected a boolean value, got null. Possibly undefined variable.", expr);
                }

                if (!b) {
                    i.remove();
                }
            }
        }

        return result;
    }
}
