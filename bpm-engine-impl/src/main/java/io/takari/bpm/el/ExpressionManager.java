package io.takari.bpm.el;

import io.takari.bpm.api.ExecutionContext;

public interface ExpressionManager {

    <T> T eval(ExecutionContext ctx, String expr, Class<T> type);

    default Object interpolate(ExecutionContext ctx, Object v) {
        return Interpolator.interpolate(this, ctx, v);
    }
}
