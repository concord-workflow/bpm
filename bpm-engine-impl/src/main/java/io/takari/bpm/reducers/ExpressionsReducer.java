package io.takari.bpm.reducers;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.EvalExpressionAction;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.BpmnErrorHelper;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Variables;
import io.takari.bpm.state.VariablesHelper;
import io.takari.bpm.utils.Timeout;
import io.takari.bpm.utils.TimeoutCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.el.ELException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ExpressionsReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(ExpressionsReducer.class);

    private final ExpressionManager expressionManager;
    private final ExecutorService executor;

    public ExpressionsReducer(ExpressionManager expressionManager, ExecutorService executor) {
        this.expressionManager = expressionManager;
        this.executor = executor;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof EvalExpressionAction)) {
            return state;
        }

        final EvalExpressionAction a = (EvalExpressionAction) action;

        Variables vars = applyInVariables(state.getVariables(), a.getIn());
        final ExecutionContextImpl ctx = new ExecutionContextImpl(expressionManager, vars);

        Callable<Command> fn = new DelegateFn(expressionManager, ctx, a.getType(), a.getExpression(), a.getDefaultCommand());

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
                throw e;
            }
        } catch (BpmnError e) {
            state = handleBpmnError(state, a, e);
        } catch (ExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new ExecutionException("Unhandled execution exception: " + a.getExpression(), e);
        }

        // we apply new state of variables regardless of whether the call was
        // successful or not
        // TODO think about it
        state = applyOutVariables(state, ctx, a.getOut());

        return state;
    }

    private Variables applyInVariables(Variables src, Set<VariableMapping> in) throws ExecutionException {
        if (in == null) {
            // if there is no IN variables, we will use the orinal process-level variables
            return src;
        }
        return VariablesHelper.copyVariables(expressionManager, src, new Variables(), in);
    }

    private ProcessInstance applyOutVariables(ProcessInstance state, ExecutionContextImpl ctx, Set<VariableMapping> out) throws ExecutionException {
        if (out != null) {
            // we need to apply actions immediately and filter the result according to
            // the supplied out variables mapping
            Variables src = ctx.toVariables();
            Variables dst = VariablesHelper.copyVariables(expressionManager, src, state.getVariables(), out);
            return state.setVariables(dst);
        }

        List<Action> actions = ctx.toActions();
        if (actions == null || actions.isEmpty()) {
            return state;
        }

        Command cmd = new PerformActionsCommand(actions);
        return state.setStack(state.getStack().push(cmd));
    }

    private ProcessInstance handleBpmnError(ProcessInstance state, EvalExpressionAction a, BpmnError e) throws ExecutionException {
        Command nextCmd = null;

        // add call point information to the error
        if (e.getDefinitionId() == null) {
            e = new BpmnError(a.getDefinitionId(), a.getElementId(), e.getErrorRef(), e.getCause());
        }

        String errorRef = e.getErrorRef();

        if (errorRef != null) {
            Map<String, Command> errors = a.getErrors();
            if (errors != null) {
                nextCmd = errors.get(errorRef);
            }
        }

        if (nextCmd == null) {
            nextCmd = a.getDefaultError();
        }

        if (nextCmd == null) {
            // no boundary error events were found - an error will be raised to
            // the parent execution

            CommandStack stack = state.getStack()
                    .push(new PerformActionsCommand(BpmnErrorHelper.raiseError(a.getDefinitionId(), a.getElementId(), errorRef, e.getCause())));
            state = state.setStack(stack);
            log.debug("handleBpmnError ['{}', '{}'] -> error will be raised", state.getBusinessKey(), a.getElementId());
        } else {
            // the element has an boundary error event - the process execution
            // will follow its flow

            CommandStack stack = state.getStack()
                    .push(new PerformActionsCommand(new SetVariableAction(ExecutionContext.LAST_ERROR_KEY, e)))
                    .push(nextCmd);
            state = state.setStack(stack);
            log.debug("handleBpmnError ['{}', '{}'] -> next command is '{}'", state.getBusinessKey(), a.getElementId(), nextCmd);
        }

        return state;
    }

    private static final class DelegateFn implements Callable<Command> {

        private final ExpressionManager expressionManager;
        private final ExecutionContext ctx;
        private final ExpressionType type;
        private final String expression;
        private final Command defaultCommand;

        public DelegateFn(ExpressionManager expressionManager, ExecutionContext ctx, ExpressionType type, String expression,
                          Command defaultCommand) {
            this.expressionManager = expressionManager;
            this.ctx = ctx;
            this.type = type;
            this.expression = expression;
            this.defaultCommand = defaultCommand;
        }

        @Override
        public Command call() throws Exception {
            Object v = expressionManager.eval(ctx, expression, Object.class);

            if (type == ExpressionType.DELEGATE) {
                if (v instanceof JavaDelegate) {
                    ((JavaDelegate) v).execute(ctx);
                } else {
                    throw new ExecutionException("Unexpected result type: " + v + ". Was expecting an instance of JavaDelegate");
                }
            }

            return defaultCommand;
        }
    }
}
