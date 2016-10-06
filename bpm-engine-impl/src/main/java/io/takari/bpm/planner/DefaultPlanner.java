package io.takari.bpm.planner;

import io.takari.bpm.Configuration;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.RemoveInstanceAction;
import io.takari.bpm.actions.SetStatusAction;
import io.takari.bpm.actions.SuspendAndPersistAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.state.Events;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.ProcessStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultPlanner implements Planner {

    private static final Logger log = LoggerFactory.getLogger(DefaultPlanner.class);

    private final DelegatingCommandHandler commandHandler;

    public DefaultPlanner(Configuration cfg) {
        this.commandHandler = new DelegatingCommandHandler(cfg);
    }

    @Override
    public List<Action> eval(ProcessInstance state) throws ExecutionException {
        List<Action> actions = new ArrayList<>();

        Command cmd = state.getStack().peek();
        log.debug("eval ['{}'] -> got '{}'", state.getBusinessKey(), cmd);

        if (cmd == null) {
            // end of the stack
            Events events = state.getEvents();
            if (events.isEmpty()) {
                // no events waiting, we are done
                actions.add(new RemoveInstanceAction(state.getId()));
                actions.add(new SetStatusAction(ProcessStatus.FINISHED));
            } else {
                // there are some events waiting
                actions.add(new SuspendAndPersistAction());
            }

            return actions;
        }

        actions = commandHandler.handle(state, cmd, actions);

        log.debug("eval ['{}'] -> done, created {} action(s): {}", state.getBusinessKey(), actions.size(), actions);
        return actions;
    }
}
