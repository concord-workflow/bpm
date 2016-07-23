package io.takari.bpm.reducers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.ProcessInstance;

public class FlowsReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(FlowsReducer.class);

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (action instanceof FollowFlowsAction) {
            FollowFlowsAction a = (FollowFlowsAction) action;

            IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
            CommandStack stack = state.getStack();
            List<SequenceFlow> flows = ProcessDefinitionUtils.findOutgoingFlows(pd, a.getElementId());
            for (SequenceFlow f : flows) {
                ProcessElementCommand cmd = new ProcessElementCommand(pd.getId(), f.getId(), a.getGroupId(), a.isExclusive());
                log.debug("reduce ['{}'] -> push '{}'", state.getBusinessKey(), cmd);
                stack = stack.push(cmd);
            }

            return state.setStack(stack);
        }

        return state;
    }
}
