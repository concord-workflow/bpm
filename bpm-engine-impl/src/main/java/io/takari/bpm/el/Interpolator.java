package io.takari.bpm.el;

import io.takari.bpm.api.ExecutionContext;

import java.util.*;

public final class Interpolator {

    private Interpolator() {
    }

    @SuppressWarnings("unchecked")
    public static Object interpolate(ExpressionManager em, ExecutionContext ctx, Object v) {
        if (v instanceof String) {
            String s = (String) v;
            if (!s.contains("${")) {
                return s;
            }
            return em.eval(ctx, s, Object.class);
        } else if (v instanceof Map) {
            return interpolateMap(em, ctx, (Map<?, ?>) v);
        } else if (v instanceof List) {
            List src = (List) v;
            if (src.isEmpty()) {
                return v;
            }

            List dst = new ArrayList(src.size());
            for (Object vv : src) {
                dst.add(interpolate(em, ctx, vv));
            }

            return dst;
        } else if (v instanceof Set) {
            Set src = (Set) v;
            if (src.isEmpty()) {
                return v;
            }

            Set dst = new HashSet(src.size());
            for (Object vv : src) {
                dst.add(interpolate(em, ctx, vv));
            }
        } if (v instanceof Object[]) {
            Object[] src = (Object[]) v;
            if (src.length == 0) {
                return v;
            }

            for (int i = 0; i < src.length; i++) {
                src[i] = interpolate(em, ctx, src[i]);
            }
        }

        return v;
    }

    private static Map<?, ?> interpolateMap(ExpressionManager em, ExecutionContext ctx, Map<?, ?> m) {
        if (m.isEmpty()) {
            return m;
        }

        Map<Object, Object> mm = new LinkedHashMap<>(m.size());
        ctx = new MapBackedExecutionContext(ctx, mm);

        for (Map.Entry<?, ?> e : m.entrySet()) {
            Object k = e.getKey();
            mm.put(k, _interpolate(em, ctx, mm, k, e.getValue()));
        }

        return mm;
    }

    private static Object _interpolate(ExpressionManager em, ExecutionContext ctx, Map<Object, Object> container, Object k, Object v) {
        if (v instanceof String) {
            String s = (String) v;
            if (s.contains("${")) {
                v = em.eval(ctx, s, Object.class);
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
                    mm.put(kk, _interpolate(em, ctx, mm, kk, e.getValue()));
                }

                v = mm;
            }
        } else if (v instanceof List) {
            List src = (List) v;
            if (!src.isEmpty()) {
                List dst = new ArrayList(src.size());
                for (Object vv : src) {
                    dst.add(_interpolate(em, ctx, null, null, vv));
                }

                v = dst;
            }
        } else if (v instanceof Set) {
            Set src = (Set) v;
            if (!src.isEmpty()) {
                Set dst = new HashSet(src.size());
                for (Object vv : src) {
                    dst.add(_interpolate(em, ctx, null, null, vv));
                }

                v = dst;
            }
        } if (v instanceof Object[]) {
            Object[] src = (Object[]) v;
            if (src.length != 0) {
                Object[] dst = new Object[src.length];
                for (int i = 0; i < src.length; i++) {
                    dst[i] = _interpolate(em, ctx, null, null, src[i]);
                }
                v = dst;
            }
        }

        return v;
    }

    private static final class MapBackedExecutionContext implements ExecutionContext {

        private final ExecutionContext delegate;
        private final Map<Object, Object> overrides;

        private MapBackedExecutionContext(ExecutionContext delegate, Map<Object, Object> overrides) {
            this.delegate = delegate;
            this.overrides = overrides;
        }

        @Override
        public Object getVariable(String key) {
            if (overrides.containsKey(key)) {
                return overrides.get(key);
            }
            return delegate.getVariable(key);
        }

        @Override
        public Map<String, Object> getVariables() {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public void setVariable(String key, Object value) {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public boolean hasVariable(String key) {
            return overrides.containsKey(key) || delegate.hasVariable(key);
        }

        @Override
        public void removeVariable(String key) {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public Set<String> getVariableNames() {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public <T> T eval(String expr, Class<T> type) {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public Map<String, Object> toMap() {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public Object interpolate(Object v) {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public void suspend(String messageRef) {
            throw new IllegalStateException("Not supported");
        }
    }
}
