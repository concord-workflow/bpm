package io.takari.bpm.elements;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.EvalExpressionAction;
import io.takari.bpm.actions.FollowFlowsAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.commands.ResumeElementCommand;
import io.takari.bpm.context.Change;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.utils.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
            Command defaultCommand = new ProcessElementCommand(pd.getId(), nextFlow.getId());

            // collect all timeout boundary events attached to this task
            List<Timeout<Command>> timeouts = ProcessDefinitionUtils.findTimers(pd, cmd);

            // create a command which will be executed after an unspecified
            // error
            Command defaultError = ProcessDefinitionUtils.findDefaultError(pd, cmd);

            // collect all boundary error events that have an errorRef
            Map<String, Command> errors = ProcessDefinitionUtils.findErrors(pd, cmd);

            Map<String, Change> ctxChangesBeforeSuspend = null;
            if (cmd instanceof ResumeElementCommand) {
                ctxChangesBeforeSuspend = ((ResumeElementCommand) cmd).getCtxChangesBeforeSuspend();
            }

            actions.add(new EvalExpressionAction.Builder(cmd.getDefinitionId(), t.getId(), t.getType(), t.getExpression(), defaultCommand)
                    .withTimeouts(timeouts)
                    .withDefaultError(defaultError)
                    .withErrors(errors)
                    .withInVariables(notEmpty(t.getIn()))
                    .withOutVariables(notEmpty(t.getOut()))
                    .withCopyAllVariables(t.isCopyAllVariables())
                    .withChanges(ctxChangesBeforeSuspend)
                    .build());

            log.debug("handle ['{}', '{}', {}, '{}'] -> done", state.getBusinessKey(), cmd.getElementId(), type, expr);
        } else {
            log.debug("handle ['{}', '{}', {}, '{}'] -> noop", state.getBusinessKey(), cmd.getElementId(), type, expr);
            actions.add(new FollowFlowsAction(cmd.getDefinitionId(), cmd.getElementId()));
        }

        return actions;
    }

    private static Set<VariableMapping> notEmpty(Set<VariableMapping> s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return s;
    }
}
