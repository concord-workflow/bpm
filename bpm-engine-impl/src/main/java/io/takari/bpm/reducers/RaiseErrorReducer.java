package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.RaiseErrorAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.ProcessInstance;

public class RaiseErrorReducer implements Reducer {

    private final ExpressionManager expressionManager;

    public RaiseErrorReducer(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
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
            cause = new ExecutionContextImpl(expressionManager, state.getVariables()).eval(expression, Throwable.class);
        }
        return state.setStack(state.getStack().push(new PerformActionsCommand(BpmnErrorHelper.raiseError(a.getErrorRef(), cause))));
    }

}
