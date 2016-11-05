package io.takari.bpm.state;

import java.util.Set;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.VariableMapping;

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
                v = sourceValue;
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

    @CoverageIgnore
    private VariablesHelper() {
    }
}
