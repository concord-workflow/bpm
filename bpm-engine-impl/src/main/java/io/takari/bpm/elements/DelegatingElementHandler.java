package io.takari.bpm.elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.CallActivity;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.EventBasedGateway;
import io.takari.bpm.model.ExclusiveGateway;
import io.takari.bpm.model.InclusiveGateway;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.ParallelGateway;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.model.SubProcess;
import io.takari.bpm.state.ProcessInstance;

public class DelegatingElementHandler implements ElementHandler {

    private static final Logger log = LoggerFactory.getLogger(DelegatingElementHandler.class);

    private static final Map<Class<? extends AbstractElement>, ElementHandler> delegates;
    static {
        delegates = new HashMap<>();
        delegates.put(StartEvent.class, new StartEventHandler());
        delegates.put(EndEvent.class, new EndEventHandler());
        delegates.put(SequenceFlow.class, new SequenceFlowHandler());
        delegates.put(ServiceTask.class, new ServiceTaskHandler());
        delegates.put(BoundaryEvent.class, new BoundaryEventHandler());
        delegates.put(SubProcess.class, new SubProcessHandler());
        delegates.put(CallActivity.class, new CallActivityHandler());
        delegates.put(IntermediateCatchEvent.class, new IntermediateCatchEventHandler());
        delegates.put(EventBasedGateway.class, new EventBasedGatewayHandler());
        delegates.put(ExclusiveGateway.class, new ExclusiveGatewayHandler());
        delegates.put(ParallelGateway.class, new ParallelGatewayHandler());
        delegates.put(InclusiveGateway.class, new InclusiveGatewayHandler());
    }

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        AbstractElement e = ProcessDefinitionUtils.findElement(pd, cmd.getElementId());

        ElementHandler delegate = delegates.get(e.getClass());
        if (delegate == null) {
            throw new ExecutionException("Unsupported element: %s", e);
        }

        actions = delegate.handle(state, cmd, actions);
        log.debug("handle ['{}', '{}'] -> done", state.getBusinessKey(), cmd.getElementId());

        return actions;
    }
}
