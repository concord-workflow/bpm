package io.takari.bpm.el;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;

public interface ExpressionManager {

    <T> T eval(ExecutionContext ctx, String expr, Class<T> type);

    default Object interpolate(ExecutionContextFactory<?> f, ExecutionContext ctx, Object v) {
        return Interpolator.interpolate(f, this, ctx, v);
    }
}
