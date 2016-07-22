package io.takari.bpm.planner;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.PopCommandAction;
import io.takari.bpm.actions.RemoveInstanceAction;
import io.takari.bpm.actions.SetStatusAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.state.EventMapHelper;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.ProcessStatus;

public class DefaultPlanner implements Planner {

    private static final Logger log = LoggerFactory.getLogger(DefaultPlanner.class);

    private final DelegatingCommandHandler commandHandler = new DelegatingCommandHandler();

    @Override
    public List<Action> eval(ProcessInstance state) throws ExecutionException {
        List<Action> actions = new ArrayList<>();

        Command cmd = state.getStack().peek();
        log.debug("eval ['{}'] -> got '{}'", state.getBusinessKey(), cmd);

        // check if we are done
        if (cmd == null) {
            actions.add(new PopCommandAction());

            if (EventMapHelper.isEmpty(state)) {
                actions.add(new SetStatusAction(ProcessStatus.FINISHED));
            } else {
                actions.add(new SetStatusAction(ProcessStatus.SUSPENDED));
            }

            if (EventMapHelper.isEmpty(state)) {
                // no one else needs this execution
                // it can be removed
                actions.add(new RemoveInstanceAction(state.getId()));
            }

            return actions;
        }

        actions = commandHandler.handle(state, cmd, actions);

        log.debug("eval ['{}'] -> done, created {} action(s): {}", state.getBusinessKey(), actions.size(), actions);
        return actions;
    }
}
