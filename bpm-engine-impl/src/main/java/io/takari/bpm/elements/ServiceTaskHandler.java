package io.takari.bpm.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.EvalExpressionAction;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.utils.Timeout;

public class ServiceTaskHandler implements ElementHandler {

    private static final Logger log = LoggerFactory.getLogger(ServiceTaskHandler.class);

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        ServiceTask t = (ServiceTask) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());

        ExpressionType type = t.getType();
        String expr = t.getExpression();

        if (expr != null && type != ExpressionType.NONE) {
            // create a command which will be executed after a normal task
            // completion
            SequenceFlow nextFlow = ProcessDefinitionUtils.findOutgoingFlow(pd, cmd.getElementId());
            Command defaultCommand = new ProcessElementCommand(pd.getId(), nextFlow.getId(), cmd.getGroupId(), cmd.isExclusive());

            // collect all timeout boundary events attached to this task
            List<Timeout<Command>> timeouts = findTimers(pd, cmd);

            // create a command which will be executed after an unspecified
            // error
            Command defaultError = findDefaultError(pd, cmd);

            // collect all boundary error events that have an errorRef
            Map<String, Command> errors = findErrors(pd, cmd);

            actions.add(new EvalExpressionAction.Builder(cmd.getDefinitionId(), t.getId(), t.getType(), t.getExpression(), defaultCommand)
                    .withTimeouts(timeouts)
                    .withDefaultError(defaultError)
                    .withErrors(errors)
                    .build());

            log.debug("handle ['{}', '{}', {}, '{}'] -> done", state.getBusinessKey(), cmd.getElementId(), type, expr);
        } else {
            log.debug("handle ['{}', '{}', {}, '{}'] -> noop", state.getBusinessKey(), cmd.getElementId(), type, expr);
            actions.add(new FollowFlowsAction(cmd.getDefinitionId(), cmd.getElementId(), cmd.getGroupId(), cmd.isExclusive()));
        }

        return actions;
    }

    private static final List<Timeout<Command>> findTimers(IndexedProcessDefinition pd, ProcessElementCommand cmd) throws ExecutionException {
        List<BoundaryEvent> events = ProcessDefinitionUtils.findOptionalBoundaryEvents(pd, cmd.getElementId());
        List<Timeout<Command>> l = new ArrayList<>(events.size());
        for (BoundaryEvent ev : events) {
            if (ev.getTimeDuration() != null) {
                Duration d = Duration.parse(ev.getTimeDuration());
                Command c = new ProcessElementCommand(pd.getId(), ev.getId(), cmd.getGroupId(), cmd.isExclusive());
                l.add(new Timeout<Command>(d.getMillis(), c));
            }
        }

        Collections.sort(l, new Comparator<Timeout<?>>() {
            @Override
            public int compare(Timeout<?> o1, Timeout<?> o2) {
                return (int) (o1.getDuration() - o2.getDuration());
            }
        });

        return l;
    }

    private static final Command findDefaultError(IndexedProcessDefinition pd, ProcessElementCommand cmd) throws ExecutionException {
        List<BoundaryEvent> events = ProcessDefinitionUtils.findOptionalBoundaryEvents(pd, cmd.getElementId());
        for (BoundaryEvent ev : events) {
            if (ev.getErrorRef() == null && ev.getTimeDuration() == null) {
                return new ProcessElementCommand(pd.getId(), ev.getId(), cmd.getGroupId(), cmd.isExclusive());
            }
        }

        return null;
    }

    private static final Map<String, Command> findErrors(IndexedProcessDefinition pd, ProcessElementCommand cmd) throws ExecutionException {
        Map<String, Command> m = new HashMap<>();

        List<BoundaryEvent> events = ProcessDefinitionUtils.findOptionalBoundaryEvents(pd, cmd.getElementId());
        for (BoundaryEvent ev : events) {
            if (ev.getErrorRef() != null && ev.getTimeDuration() == null) {
                m.put(ev.getErrorRef(), new ProcessElementCommand(pd.getId(), ev.getId(), cmd.getGroupId(), cmd.isExclusive()));
            }
        }

        return m;
    }
}
