package io.takari.bpm.reducers;

import io.takari.bpm.Configuration;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.EvalExpressionAction;
import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Variables;
import io.takari.bpm.state.VariablesHelper;
import io.takari.bpm.utils.Timeout;
import io.takari.bpm.utils.TimeoutCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.ELException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ExpressionsReducer extends BpmnErrorHandlingReducer {

    private static final Logger log = LoggerFactory.getLogger(ExpressionsReducer.class);

    private final Configuration cfg;
    private final ExpressionManager expressionManager;
    private final ExecutorService executor;

    public ExpressionsReducer(Configuration cfg, ExpressionManager expressionManager, ExecutorService executor) {
        super(cfg);
        this.cfg = cfg;
        this.expressionManager = expressionManager;
        this.executor = executor;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof EvalExpressionAction)) {
            return state;
        }

        final EvalExpressionAction a = (EvalExpressionAction) action;

        Variables vars = VariablesHelper.applyInVariables(expressionManager, state.getVariables(), a.getIn());
        final ExecutionContextImpl ctx = new ExecutionContextImpl(expressionManager, vars);

        boolean storeResult = cfg.isStoreExpressionEvalResultsInContext();
        Callable<Command> fn = new DelegateFn(expressionManager, ctx, a.getType(), a.getExpression(), a.getDefaultCommand(), storeResult);

        List<Timeout<Command>> timeouts = a.getTimeouts();
        if (timeouts != null && !timeouts.isEmpty()) {
            // a timeout handling decorator
            fn = new TimeoutCallable<>(executor, timeouts, fn);
        }

        try {
            Command result = fn.call();
            log.debug("reduce ['{}', '{}'] -> next action is '{}'", state.getBusinessKey(), a, result);

            CommandStack stack = state.getStack();
            state = state.setStack(stack.push(result));
        } catch (ELException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BpmnError) {
                state = handleBpmnError(state, a, (BpmnError) cause);
            } else {
                state = handleException(state, a, e);
            }
        } catch (BpmnError e) {
            state = handleBpmnError(state, a, e);
        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            state = handleException(state, a, e);
        }

        // we apply new state of variables regardless of whether the call was
        // successful or not
        state = VariablesHelper.applyOutVariables(expressionManager, state, ctx, a.getOut());

        return state;
    }

    private ProcessInstance handleException(ProcessInstance state, EvalExpressionAction a, Exception e) throws ExecutionException {
        return handleException(state, a.getDefinitionId(), a.getElementId(), e, a.getErrors(), a.getDefaultError());
    }

    private ProcessInstance handleBpmnError(ProcessInstance state, EvalExpressionAction a, BpmnError e) throws ExecutionException {
        return handleBpmnError(state, a.getDefinitionId(), a.getElementId(), e, a.getErrors(), a.getDefaultError());
    }

    private static final class DelegateFn implements Callable<Command> {

        private final ExpressionManager expressionManager;
        private final ExecutionContext ctx;
        private final ExpressionType type;
        private final String expression;
        private final Command defaultCommand;
        private final boolean storeResult;

        public DelegateFn(ExpressionManager expressionManager, ExecutionContext ctx, ExpressionType type,
                          String expression, Command defaultCommand, boolean storeResult) {
            this.expressionManager = expressionManager;
            this.ctx = ctx;
            this.type = type;
            this.expression = expression;
            this.defaultCommand = defaultCommand;
            this.storeResult = storeResult;
        }

        @Override
        public Command call() throws Exception {
            Object v = expressionManager.eval(ctx, expression, Object.class);

            if (type == ExpressionType.DELEGATE) {
                if (v instanceof JavaDelegate) {
                    ((JavaDelegate) v).execute(ctx);

                    if (storeResult) {
                        ctx.setVariable(ServiceTask.EXPRESSION_RESULT_VAR, null);
                    }
                } else {
                    throw new ExecutionException("Unexpected result type: " + v + ". Was expecting an instance of JavaDelegate");
                }
            }

            if (storeResult) {
                ctx.setVariable(ServiceTask.EXPRESSION_RESULT_VAR, v);
            }

            return defaultCommand;
        }
    }
}
