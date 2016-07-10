package io.takari.bpm.planner;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.SuspendAndPersistAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessEventMappingCommand;
import io.takari.bpm.state.EventMapHelper;
import io.takari.bpm.state.ProcessInstance;

public class ProcessEventMappingCommandHandler implements CommandHandler<ProcessEventMappingCommand> {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessEventMappingCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        if (!EventMapHelper.isEmpty(state, cmd.getDefinitionId())) {
            actions.add(new SuspendAndPersistAction());
        }

        return actions;
    }
}
