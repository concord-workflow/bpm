package io.takari.bpm.state;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.VariableMapping;

import java.util.List;
import java.util.Set;

public final class VariablesHelper {

    public static Variables copyVariables(ExpressionManager em, Variables src, Variables dst, Set<VariableMapping> mapping)
            throws ExecutionException {

        if (mapping == null) {
            return dst;
        }

        for (VariableMapping m : mapping) {
            String source = m.getSource();
            String sourceExpression = m.getSourceExpression();
            Object sourceValue = m.getSourceValue();

            Object v = null;
            if (sourceValue != null) {
                if (m.isInterpolateValue()) {
                    ExecutionContextImpl ctx = new ExecutionContextImpl(em, src);
                    v = em.interpolate(ctx, sourceValue);
                } else {
                    v = sourceValue;
                }
            } else if (source != null) {
                v = src.getVariable(source);
            } else if (sourceExpression != null) {
                ExecutionContextImpl ctx = new ExecutionContextImpl(em, src);
                v = em.eval(ctx, sourceExpression, Object.class);
            }

            dst = dst.setVariable(m.getTarget(), v);
        }

        return dst;
    }

    public static Variables copyVariables(Variables src, Variables dst) {
        Set<String> keys = src.getVariableNames();
        for (String k : keys) {
            Object v = src.getVariable(k);
            dst = dst.setVariable(k, v);
        }
        return dst;
    }

    public static Variables applyInVariables(ExpressionManager expressionManager, Variables src,
                                             Set<VariableMapping> in) throws ExecutionException {

        if (in == null) {
            // if there is no IN variables, we will use the original process-level variables
            return src;
        }
        return copyVariables(expressionManager, src, new Variables(), in);
    }

    public static ProcessInstance applyOutVariables(ExpressionManager expressionManager, ProcessInstance state,
                                                    ExecutionContextImpl ctx, Set<VariableMapping> out) throws ExecutionException {

        if (out != null) {
            // we need to apply actions immediately and filter the result according to
            // the supplied out variables mapping
            Variables src = ctx.toVariables();
            Variables dst = copyVariables(expressionManager, src, state.getVariables(), out);
            return state.setVariables(dst);
        }

        // otherwise, we just queue the necessary changes on the stack
        List<Action> actions = ctx.toActions();
        if (actions == null || actions.isEmpty()) {
            return state;
        }

        Command cmd = new PerformActionsCommand(actions);
        return state.setStack(state.getStack().push(cmd));
    }

    @CoverageIgnore
    private VariablesHelper() {
    }
}
