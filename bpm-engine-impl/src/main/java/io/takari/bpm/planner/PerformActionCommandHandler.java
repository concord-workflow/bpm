package io.takari.bpm.planner;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.state.ProcessInstance;

public class PerformActionCommandHandler implements CommandHandler<PerformActionsCommand> {

    @Override
    public List<Action> handle(ProcessInstance state, PerformActionsCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());
        actions.addAll(cmd.getActions());
        return actions;
    }
}
