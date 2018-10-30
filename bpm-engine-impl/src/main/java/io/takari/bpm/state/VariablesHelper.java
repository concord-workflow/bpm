package io.takari.bpm.state;

import io.takari.bpm.actions.Action;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.api.Variables;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.VariableMapping;

import javax.el.PropertyNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VariablesHelper {

    public static Variables copyVariables(ExecutionContextFactory<?> contextFactory, Variables src, Variables dst, Set<VariableMapping> mapping) {
        return copyVariables(contextFactory, src, dst, mapping, false);
    }

    public static Variables copyVariables(ExecutionContextFactory<?> contextFactory, Variables src,
                                          Variables dst, Set<VariableMapping> mapping,
                                          boolean ignoreMappingErrors) {

        if (mapping == null) {
            return dst;
        }

        for (VariableMapping m : mapping) {
            String source = m.getSource();
            String sourceExpression = m.getSourceExpression();
            Object sourceValue = m.getSourceValue();

            try {
                Object v = null;
                if (sourceValue != null) {
                    if (m.isInterpolateValue()) {
                        ExecutionContext ctx = contextFactory.create(src);
                        v = ctx.interpolate(sourceValue);
                    } else {
                        v = sourceValue;
                    }
                } else if (source != null) {
                    v = src.getVariable(source);
                } else if (sourceExpression != null) {
                    ExecutionContext ctx = contextFactory.create(src);
                    v = ctx.eval(sourceExpression, Object.class);
                }

                dst = dst.setVariable(m.getTarget(), v);
            } catch (PropertyNotFoundException e) {
                if (!ignoreMappingErrors) {
                    throw e;
                }
            }
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

    public static Variables applyInVariables(ExecutionContextFactory<?> contextFactory, Variables src,
                                             Set<VariableMapping> in,
                                             boolean appendCurrentVariablesIntoInVariables) {

        if (in == null) {
            // if there is no IN variables, we will use the original process-level variables
            return src;
        }

        Variables parentVariables = null;
        if(appendCurrentVariablesIntoInVariables) {
            parentVariables = src;
        }
        return copyVariables(contextFactory, src, new Variables(parentVariables), in);
    }

    public static ProcessInstance applyOutVariables(ExecutionContextFactory<?> contextFactory, ProcessInstance state,
                                                    ExecutionContextImpl ctx, Set<VariableMapping> out) {
        return applyOutVariables(contextFactory, state, ctx, out, false);
    }

    public static ProcessInstance applyOutVariables(ExecutionContextFactory<?> contextFactory, ProcessInstance state,
                                                    ExecutionContextImpl ctx, Set<VariableMapping> out,
                                                    boolean ignoreMappingErrors) {

        if (ctx == null) {
            return state;
        }

        if (out != null) {
            // we need to apply actions immediately and filter the result according to
            // the supplied out variables mapping
            Variables src = ctx.toVariables();
            Variables dst = copyVariables(contextFactory, src, state.getVariables(), out, ignoreMappingErrors);
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

    @SuppressWarnings("unchecked")
    public static ProcessInstance interpolate(ExecutionContextFactory<?> contextFactory, ProcessInstance state) {
        Variables vars = state.getVariables();

        ExecutionContext ctx = contextFactory.create(vars);

        Map<String, Object> m = new LinkedHashMap<>(vars.asMap());
        m = (Map<String, Object>) ctx.interpolate(m);

        Variables interpolated = new Variables(vars.getParent()).setVariables(m);
        return state.setVariables(interpolated);
    }

    @CoverageIgnore
    private VariablesHelper() {
    }
}
