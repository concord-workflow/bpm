package io.takari.bpm.planner;

import io.takari.bpm.Configuration;
import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.*;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;

public class DelegatingCommandHandler implements CommandHandler<Command> {

    private final ProcessElementCommandHandler processElementCommandHandler;
    private final PerformActionCommandHandler performActionCommandHandler;
    private final ActivityFinalizerCommandHandler activityFinalizerCommandHandler;
    private final MergeVariablesCommandHandler mergeVariablesCommandHandler;

    public DelegatingCommandHandler(Configuration cfg) {
        this.processElementCommandHandler = new ProcessElementCommandHandler();
        this.performActionCommandHandler = new PerformActionCommandHandler();
        this.activityFinalizerCommandHandler = new ActivityFinalizerCommandHandler(cfg);
        this.mergeVariablesCommandHandler = new MergeVariablesCommandHandler();
    }

    @Override
    public List<Action> handle(ProcessInstance state, Command cmd, List<Action> actions) throws ExecutionException {
        if (cmd instanceof ProcessElementCommand) {
            return processElementCommandHandler.handle(state, (ProcessElementCommand) cmd, actions);
        } else if (cmd instanceof PerformActionsCommand) {
            return performActionCommandHandler.handle(state, (PerformActionsCommand) cmd, actions);
        } else if (cmd instanceof ActivityFinalizerCommand) {
            return activityFinalizerCommandHandler.handle(state, (ActivityFinalizerCommand) cmd, actions);
        } else if (cmd instanceof MergeVariablesCommand) {
            return mergeVariablesCommandHandler.handle(state, (MergeVariablesCommand) cmd, actions);
        } else {
            throw new ExecutionException("Unsupported command: %s", cmd);
        }
    }
}
