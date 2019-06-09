package io.takari.bpm.el;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;

import java.util.*;

public final class Interpolator {

    private Interpolator() {
    }

    public static Object interpolate(ExecutionContextFactory<?> f, ExpressionManager em, ExecutionContext ctx, Object v) {
        return interpolate(new ExecutionContextEvaluator(f, em), ctx, v);
    }

    @SuppressWarnings("unchecked")
    public static <T> Object interpolate(Evaluator<T> evaluator, T ctx, Object v) {
        if (v instanceof String) {
            String s = (String) v;
            if (!evaluator.hasExpression(s)) {
                return s;
            }
            return evaluator.eval(ctx, s);
        } else if (v instanceof Map) {
            return interpolateMap(evaluator, ctx, (Map<?, ?>) v);
        } else if (v instanceof List) {
            List src = (List) v;
            if (src.isEmpty()) {
                return v;
            }

            List dst = new ArrayList(src.size());
            for (Object vv : src) {
                dst.add(interpolate(evaluator, ctx, vv));
            }

            return dst;
        } else if (v instanceof Set) {
            Set src = (Set) v;
            if (src.isEmpty()) {
                return v;
            }

            Set dst = new HashSet(src.size());
            for (Object vv : src) {
                dst.add(interpolate(evaluator, ctx, vv));
            }

            return dst;
        }
        if (v instanceof Object[]) {
            Object[] src = (Object[]) v;
            if (src.length == 0) {
                return v;
            }

            for (int i = 0; i < src.length; i++) {
                src[i] = interpolate(evaluator, ctx, src[i]);
            }
        }

        return v;
    }

    private static <T> Map<?, ?> interpolateMap(Evaluator<T> evaluator, T ctx, Map<?, ?> m) {
        if (m.isEmpty()) {
            return m;
        }

        Map<Object, Object> mm = new LinkedHashMap<>(m.size());
        ctx = evaluator.withOverrides(ctx, mm);

        for (Map.Entry<?, ?> e : m.entrySet()) {
            Object k = e.getKey();
            mm.put(k, _interpolate(evaluator, ctx, mm, k, e.getValue(), mm));
        }

        return mm;
    }

    @SuppressWarnings("unchecked")
    private static <T> Object _interpolate(Evaluator<T> evaluator, T ctx, Map<Object, Object> container, Object k, Object v, Map<Object, Object> overrides) {
        if (v instanceof String) {
            String s = (String) v;
            if (evaluator.hasExpression(s)) {
                v = evaluator.eval(ctx, s);
            }
        } else if (v instanceof Map) {
            Map<Object, Object> m = (Map<Object, Object>) v;
            if (!m.isEmpty()) {
                Map<Object, Object> mm = new LinkedHashMap<>(m);
                if (container != null) {
                    container.put(k, mm);
                }

                for (Map.Entry<Object, Object> e : m.entrySet()) {
                    Object kk = e.getKey();
                    Object interpolatedValue = _interpolate(evaluator, ctx, mm, kk, e.getValue(), overrides);
                    if (contains(mm, interpolatedValue)) {
                        // got cycle, try outer variable
                        Map<Object, Object> oldOverrides = new HashMap<>(overrides);
                        overrides.clear();
                        interpolatedValue = _interpolate(evaluator, ctx, mm, kk, e.getValue(), overrides);
                        overrides.putAll(oldOverrides);
                    }
                    mm.put(kk, interpolatedValue);
                }

                v = mm;
            }
        } else if (v instanceof List) {
            List src = (List) v;
            if (!src.isEmpty()) {
                List dst = new ArrayList(src.size());
                for (Object vv : src) {
                    dst.add(_interpolate(evaluator, ctx, null, null, vv, overrides));
                }

                v = dst;
            }
        } else if (v instanceof Set) {
            Set src = (Set) v;
            if (!src.isEmpty()) {
                Set dst = new HashSet(src.size());
                for (Object vv : src) {
                    dst.add(_interpolate(evaluator, ctx, null, null, vv, overrides));
                }

                v = dst;
            }
        }
        if (v instanceof Object[]) {
            Object[] src = (Object[]) v;
            if (src.length != 0) {
                Object[] dst = new Object[src.length];
                for (int i = 0; i < src.length; i++) {
                    dst[i] = _interpolate(evaluator, ctx, null, null, src[i], overrides);
                }
                v = dst;
            }
        }

        return v;
    }

    @SuppressWarnings("unchecked")
    private static boolean contains(Object o, Object interpolatedValue) {
        if (o == interpolatedValue) {
            return true;
        }

        if (interpolatedValue instanceof Map) {
            for (Map.Entry<Object, Object> e : ((Map<Object, Object>) interpolatedValue).entrySet()) {
                if (contains(o, e.getValue())) {
                    return true;
                }
            }
        } else if (interpolatedValue instanceof Collection) {
            for (Object e : (Collection<Object>) interpolatedValue) {
                if (contains(o, e)) {
                    return true;
                }
            }
        } else if (interpolatedValue instanceof Object[]) {
            for (Object e : (Object[]) interpolatedValue) {
                if (contains(o, e)) {
                    return true;
                }
            }
        }

        return false;
    }

    public interface Evaluator<T> {

        boolean hasExpression(String v);

        Object eval(T ctx, String v);

        T withOverrides(T ctx, Map<Object, Object> overrides);
    }

    private static class ExecutionContextEvaluator implements Evaluator<ExecutionContext> {

        private final ExecutionContextFactory<? extends ExecutionContext> ctxFactory;
        private final ExpressionManager expressionManager;

        private ExecutionContextEvaluator(ExecutionContextFactory<? extends ExecutionContext> ctxFactory, ExpressionManager expressionManager) {
            this.ctxFactory = ctxFactory;
            this.expressionManager = expressionManager;
        }

        @Override
        public boolean hasExpression(String v) {
            return v.contains("${");
        }

        @Override
        public Object eval(ExecutionContext ctx, String v) {
            return expressionManager.eval(ctx, v, Object.class);
        }

        @Override
        public ExecutionContext withOverrides(ExecutionContext ctx, Map<Object, Object> overrides) {
            return ctxFactory.withOverrides(ctx, overrides);
        }
    }
}
