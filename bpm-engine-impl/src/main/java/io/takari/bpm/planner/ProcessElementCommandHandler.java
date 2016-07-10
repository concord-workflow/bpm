package io.takari.bpm.planner;

import java.util.List;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ActivateElementAction;
import io.takari.bpm.actions.FireOnElementInterceptorsAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.elements.DelegatingElementHandler;
import io.takari.bpm.elements.ElementHandler;
import io.takari.bpm.state.ProcessInstance;

public class ProcessElementCommandHandler implements CommandHandler<ProcessElementCommand> {

    private final ElementHandler elementHandler = new DelegatingElementHandler();

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        ProcessElementCommand c = (ProcessElementCommand) cmd;
        actions = elementHandler.handle(state, c, actions);
        actions.add(new ActivateElementAction(c.getDefinitionId(), c.getElementId()));
        actions.add(new FireOnElementInterceptorsAction(c.getDefinitionId(), c.getElementId()));
        return actions;
    }

}
