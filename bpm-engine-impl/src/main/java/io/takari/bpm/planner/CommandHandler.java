package io.takari.bpm.planner;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.state.ProcessInstance;

public interface CommandHandler<T extends Command> {

    List<Action> handle(ProcessInstance state, T cmd, List<Action> actions) throws ExecutionException;
}
