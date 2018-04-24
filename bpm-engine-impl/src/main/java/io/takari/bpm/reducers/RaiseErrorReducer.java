package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.RaiseErrorAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.context.ExecutionContextFactory;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.ProcessInstance;

public class RaiseErrorReducer implements Reducer {

    private final ExecutionContextFactory<?> contextFactory;

    public RaiseErrorReducer(ExecutionContextFactory<?> contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof RaiseErrorAction)) {
            return state;
        }

        RaiseErrorAction a = (RaiseErrorAction) action;
        String expression = a.getCauseExpression();
        Throwable cause = null;
        if (expression != null) {
            ExecutionContext ctx = contextFactory.create(state.getVariables(), a.getDefinitionId(), a.getElementId());
            cause = ctx.eval(expression, Throwable.class);
        }

        return state.setStack(state.getStack().push(new PerformActionsCommand(
                BpmnErrorHelper.raiseError(a.getDefinitionId(), a.getElementId(), a.getErrorRef(), cause))));
    }
}
