package io.takari.bpm.planner;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.MergeVariablesAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.SetVariablesAction;
import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.Variables;
import io.takari.bpm.commands.MergeVariablesCommand;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.Frames;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;

public class MergeVariablesCommandHandler implements CommandHandler<MergeVariablesCommand> {

    @Override
    public List<Action> handle(ProcessInstance state, MergeVariablesCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        Variables target;
        if (Frames.isCurrentScopeOwner(state)) {
            Scopes scopes = state.getScopes();
            if (!state.getEvents().isEmpty(scopes, scopes.getCurrentId())) {
                // the child process is suspended, the variables will be merged when it's done
                return actions;
            }

            // merge into the current variables of the parent
            target = Frames.getParentVariables(state);
        } else {
            // a state created by a previous version: the variables were captured when the child was called
            target = cmd.getTarget();
        }

        // variables map of the child process
        Variables source = state.getVariables();

        // make parent process' variables as our current
        actions.add(new SetVariablesAction(target));

        // merge out variables of the child process into the parent's (current)
        // variables map
        actions.add(new MergeVariablesAction(source, cmd.getOutVariables(), cmd.isCopyAllVariables()));

        // handle an raised error (see also EndEventHandler)
        BpmnError error = BpmnErrorHelper.getRaisedError(source);
        if (error != null) {
            actions.add(BpmnErrorHelper.raiseError(error));
        }

        return actions;
    }
}
