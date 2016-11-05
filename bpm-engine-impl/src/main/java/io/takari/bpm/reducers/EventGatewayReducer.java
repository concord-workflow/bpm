package io.takari.bpm.reducers;

import com.google.common.collect.Lists;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EventGatewayReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(EventGatewayReducer.class);

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof ProcessEventGatewayAction)) {
            return state;
        }

        ProcessEventGatewayAction a = (ProcessEventGatewayAction) action;

        CommandStack stack = state.getStack();

        // close the (future) scope right after the events
        stack = stack.push(new PerformActionsCommand(new PopScopeAction()));

        // find all events and create actions to handle them
        IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
        List<SequenceFlow> flows = ProcessDefinitionUtils.findOptionalOutgoingFlows(pd, a.getElementId());
        if (flows == null || flows.isEmpty()) {
            throw new ExecutionException("Invalid process definition '%s': event gateway '%s' must contain at least one event", a.getDefinitionId(), a.getElementId());
        }

        for (SequenceFlow f : Lists.reverse(flows)) {
            if (f.getExpression() != null) {
                log.warn("reduce ['{}', '{}', '{}'] -> flow expressions are not supported for event gateways", state.getBusinessKey(), a);
            }

            AbstractElement e = ProcessDefinitionUtils.findElement(pd, f.getTo());
            if (!(e instanceof IntermediateCatchEvent)) {
                throw new ExecutionException("Invalid process definition '%s': event gateway '%s' must contain only intermediate catch events", a.getDefinitionId(), a.getElementId());
            }

            // create the event processing action
            stack = stack.push(new PerformActionsCommand(new CreateEventAction(a.getDefinitionId(), e.getId())))
                    .push(new PerformActionsCommand(new ActivateElementAction(a.getDefinitionId(), e.getId())));

            // manually activate the flow
            stack = stack.push(new PerformActionsCommand(new ActivateElementAction(a.getDefinitionId(), f.getId())));
        }

        // start a new scope
        stack = stack.push(new PerformActionsCommand(new PushScopeAction(true)));

        return state.setStack(stack);
    }
}
