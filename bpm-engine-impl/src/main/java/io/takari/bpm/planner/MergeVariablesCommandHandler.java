package io.takari.bpm.planner;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.MergeVariablesAndEventMapAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.SetVariablesAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.MergeVariablesCommand;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Variables;

public class MergeVariablesCommandHandler implements CommandHandler<MergeVariablesCommand> {

    @Override
    public List<Action> handle(ProcessInstance state, MergeVariablesCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        // variables map of the child process
        Variables source = state.getVariables();

        // this will be a merged map of variables from both parent and child
        // processes
        Variables target = cmd.getTarget();

        // make parent process' variables as our current
        actions.add(new SetVariablesAction(target));

        // merge out variables of the child process into the parent's (current)
        // variables map
        actions.add(new MergeVariablesAndEventMapAction(source, cmd.getOutVariables()));

        // handle an raised error (see also EndEventHandler)
        String errorRef = BpmnErrorHelper.getRaisedError(source);
        if (errorRef != null) {
            actions.add(BpmnErrorHelper.raiseError(errorRef));
        }

        return actions;
    }
}
