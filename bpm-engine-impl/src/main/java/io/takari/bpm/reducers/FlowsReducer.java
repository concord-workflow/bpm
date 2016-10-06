package io.takari.bpm.reducers;

import com.google.common.collect.Lists;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;

public class FlowsReducer implements Reducer {

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof FollowFlowsAction)) {
            return state;
        }

        FollowFlowsAction a = (FollowFlowsAction) action;

        IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
        CommandStack stack = state.getStack();

        List<SequenceFlow> flows;
        if (a.getFlowIds() != null) {
            // we have a predefined set of flows to follow
            flows = ProcessDefinitionUtils.findFlows(pd, a.getFlowIds());
        } else {
            // we need to find a set of outgoing flows to follow
            flows = ProcessDefinitionUtils.findOutgoingFlows(pd, a.getElementId());
        }

        // reversed order is expected
        for (SequenceFlow f : Lists.reverse(flows)) {
            ProcessElementCommand cmd = new ProcessElementCommand(pd.getId(), f.getId());
            stack = stack.push(cmd);
        }

        return state.setStack(stack);
    }
}
