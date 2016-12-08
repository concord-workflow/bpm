package io.takari.bpm.planner;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.ActivateElementAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.elements.DelegatingElementHandler;
import io.takari.bpm.elements.ElementHandler;
import io.takari.bpm.state.ProcessInstance;

import java.util.List;

public class ProcessElementCommandHandler implements CommandHandler<ProcessElementCommand> {

    private final ElementHandler elementHandler = new DelegatingElementHandler();

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions = elementHandler.handle(state, cmd, actions);
        actions.add(new ActivateElementAction(cmd.getDefinitionId(), cmd.getElementId(), 1));
        return actions;
    }

}
