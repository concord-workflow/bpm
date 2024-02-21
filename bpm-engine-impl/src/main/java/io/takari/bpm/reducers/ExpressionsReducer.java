package io.takari.bpm.reducers;

import io.takari.bpm.Configuration;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.EvalExpressionAction;
import io.takari.bpm.api.*;
import io.takari.bpm.commands.Command;
import io.takari.bpm.context.ContextUtils;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.VariablesHelper;
import io.takari.bpm.task.JavaDelegateHandler;
import io.takari.bpm.utils.Timeout;
import io.takari.bpm.utils.TimeoutCallable;

import javax.el.ELException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ExpressionsReducer extends BpmnErrorHandlingReducer {

    private final ExecutionContextFactory<? extends ExecutionContextImpl> contextFactory;
    private final Configuration cfg;
    private final JavaDelegateHandler javaDelegateHandler;
    private final ExecutorService executor;

    public ExpressionsReducer(ExecutionContextFactory<? extends ExecutionContextImpl> contextFactory,
                              Configuration cfg,
                              JavaDelegateHandler javaDelegateHandler,
                              ExecutorService executor) {

        super(cfg);

        this.contextFactory = contextFactory;
        this.cfg = cfg;
        this.javaDelegateHandler = javaDelegateHandler;
        this.executor = executor;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof EvalExpressionAction)) {
            return state;
        }

        final EvalExpressionAction a = (EvalExpressionAction) action;

        boolean hasError = false;
        ExecutionContextImpl ctx = null;
        try {
            Variables vars = VariablesHelper.applyInVariables(contextFactory, state.getVariables(), a.getIn(), a.isCopyAllVariables());
            ctx = contextFactory.create(vars, a.getDefinitionId(), a.getElementId());

            boolean storeResult = cfg.isStoreExpressionEvalResultsInContext();
            Callable<Command> fn = new DelegateFn(javaDelegateHandler, ctx, a.getType(), a.getExpression(), a.getDefaultCommand(), storeResult);

            List<Timeout<Command>> timeouts = a.getTimeouts();
            if (timeouts != null && !timeouts.isEmpty()) {
                // a timeout handling decorator
                fn = new TimeoutCallable<>(executor, timeouts, fn);
            }

            Command next = fn.call();
            state = ContextUtils.handleSuspend(state ,ctx, a.getDefinitionId(), a.getElementId(), next);
        } catch (ELException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BpmnError) {
                state = handleBpmnError(state, a, (BpmnError) cause);
            } else {
                state = handleException(state, a, e);
            }
            hasError = true;
        } catch (BpmnError e) {
            state = handleBpmnError(state, a, e);
            hasError = true;
        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            state = handleException(state, a, e);
            hasError = true;
        }

        // we apply new state of variables regardless of whether the call was
        // successful or not
        boolean resumeFromSameStep = ctx != null && ctx.getSuspendMessageRef() != null && ctx.isResumeFromSameStep();
        if (!resumeFromSameStep) {
            boolean ignoreMappingErrors = hasError;
            state = VariablesHelper.applyOutVariables(contextFactory, state, ctx, a.getOut(), ignoreMappingErrors);
        }

        return state;
    }

    private ProcessInstance handleException(ProcessInstance state, EvalExpressionAction a, Exception e) throws ExecutionException {
        return handleException(state, a.getDefinitionId(), a.getElementId(), e, a.getErrors(), a.getDefaultError());
    }

    private ProcessInstance handleBpmnError(ProcessInstance state, EvalExpressionAction a, BpmnError e) throws ExecutionException {
        return handleBpmnError(state, a.getDefinitionId(), a.getElementId(), e, a.getErrors(), a.getDefaultError());
    }

    private static final class DelegateFn implements Callable<Command> {

        private final JavaDelegateHandler javaDelegateHandler;
        private final ExecutionContext ctx;
        private final ExpressionType type;
        private final String expression;
        private final Command defaultCommand;
        private final boolean storeResult;

        public DelegateFn(JavaDelegateHandler javaDelegateHandler,
                          ExecutionContext ctx,
                          ExpressionType type,
                          String expression,
                          Command defaultCommand,
                          boolean storeResult) {

            this.javaDelegateHandler = javaDelegateHandler;
            this.ctx = ctx;
            this.type = type;
            this.expression = expression;
            this.defaultCommand = defaultCommand;
            this.storeResult = storeResult;
        }

        @Override
        public Command call() throws Exception {
            Object v = ctx.eval(expression, Object.class);

            if (type == ExpressionType.DELEGATE) {
                javaDelegateHandler.execute(v, ctx);
            } else {
                if (storeResult) {
                    ctx.setVariable(ServiceTask.EXPRESSION_RESULT_VAR, v);
                }
            }

            return defaultCommand;
        }
    }
}
