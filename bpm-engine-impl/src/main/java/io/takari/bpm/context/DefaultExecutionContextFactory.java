package io.takari.bpm.context;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.api.Variables;
import io.takari.bpm.el.ExpressionManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultExecutionContextFactory implements ExecutionContextFactory<ExecutionContextImpl> {

    private final ExpressionManager expressionManager;

    public DefaultExecutionContextFactory(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
    }

    @Override
    public ExecutionContextImpl create(Variables source) {
        return new ExecutionContextImpl(this, expressionManager, source);
    }

    @Override
    public ExecutionContextImpl create(Variables source, String processDefinitionId, String elementId) {
        return new ExecutionContextImpl(this, expressionManager, source, processDefinitionId, elementId);
    }

    @Override
    public ExecutionContext withOverrides(ExecutionContext delegate, Map<Object, Object> overrides) {
        return new MapBackedExecutionContext(this, expressionManager, delegate, overrides);
    }

    public static class MapBackedExecutionContext implements ExecutionContext {

        private final ExecutionContextFactory<? extends ExecutionContext> executionContextFactory;
        private final ExpressionManager exprManager;

        private final ExecutionContext delegate;
        private final Map<Object, Object> overrides;

        public MapBackedExecutionContext(
                ExecutionContextFactory<? extends ExecutionContext> executionContextFactory,
                ExpressionManager exprManager,
                ExecutionContext delegate, Map<Object, Object> overrides) {
            this.executionContextFactory = executionContextFactory;
            this.exprManager = exprManager;
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
            Set<String> result = new HashSet<>(delegate.getVariableNames());
            result.addAll(overrides.keySet().stream()
                    .filter(k -> k instanceof String)
                    .map(k -> (String)k)
                    .collect(Collectors.toSet()));
            return result;
        }

        @Override
        public <T> T eval(String expr, Class<T> type) {
            return exprManager.eval(this, expr, type);
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> result = new HashMap<>(delegate.toMap());
            for(Map.Entry<Object, Object> e : overrides.entrySet()) {
                if (e.getKey() instanceof String) {
                    result.put((String) e.getKey(), e.getValue());
                }
            }
            return result;
        }

        @Override
        public Object interpolate(Object v) {
            return exprManager.interpolate(executionContextFactory, this, v);
        }

        @Override
        public void suspend(String messageRef) {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public void suspend(String messageRef, Object payload) {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public String getProcessDefinitionId() {
            return delegate.getProcessDefinitionId();
        }

        @Override
        public String getElementId() {
            return delegate.getElementId();
        }
    }
}
