package io.takari.bpm.reducers;

import io.takari.bpm.Configuration;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.CreateEventAction;
import io.takari.bpm.actions.EvalExpressionAction;
import io.takari.bpm.api.*;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.ServiceTask;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.VariablesHelper;
import io.takari.bpm.task.JavaDelegateHandler;
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

        Variables vars = VariablesHelper.applyInVariables(contextFactory, state.getVariables(), a.getIn(), a.isCopyAllVariables());
        final ExecutionContextImpl ctx = contextFactory.create(vars, a.getDefinitionId(), a.getElementId());

        boolean storeResult = cfg.isStoreExpressionEvalResultsInContext();
        Callable<Command> fn = new DelegateFn(javaDelegateHandler, ctx, a.getType(), a.getExpression(), a.getDefaultCommand(), storeResult);

        List<Timeout<Command>> timeouts = a.getTimeouts();
        if (timeouts != null && !timeouts.isEmpty()) {
            // a timeout handling decorator
            fn = new TimeoutCallable<>(executor, timeouts, fn);
        }

        try {
            Command result = fn.call();

            CommandStack stack = state.getStack();

            String messageRef = ctx.getSuspendMessageRef();
            if (messageRef == null) {
                log.debug("reduce ['{}', '{}'] -> next action is '{}'", state.getBusinessKey(), a, result);
                stack = stack.push(result);
            } else {
                log.debug("reduce ['{}', '{}'] -> suspend is requested '{}'", state.getBusinessKey(), a, messageRef);
                stack = stack.push(new PerformActionsCommand(
                        new CreateEventAction(a.getDefinitionId(), a.getElementId(), messageRef, null, null, null)));
            }

            state = state.setStack(stack);
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
        state = VariablesHelper.applyOutVariables(contextFactory, state, ctx, a.getOut());

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
