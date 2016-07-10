package io.takari.bpm.planner;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ClearCommandsAction;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ClearCommandStackCommand;
import io.takari.bpm.state.EventMapHelper;
import io.takari.bpm.state.ProcessInstance;

public class ClearCommandStartCommandHandler implements CommandHandler<ClearCommandStackCommand> {

    @Override
    public List<Action> handle(ProcessInstance state, ClearCommandStackCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        if (!EventMapHelper.isEmpty(state)) {
            // if there is more events waiting, we need to clear out current
            // stack so the execution will stop after the closing gateway
            actions.add(new ClearCommandsAction());
        }

        return actions;
    }
}
