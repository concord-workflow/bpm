package io.takari.bpm.context;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.api.Variables;
import io.takari.bpm.el.ExpressionManager;

import java.util.Map;
import java.util.Set;

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
        return new MapBackedExecutionContext(delegate, overrides);
    }

    public static class MapBackedExecutionContext implements ExecutionContext {

        private final ExecutionContext delegate;
        private final Map<Object, Object> overrides;

        public MapBackedExecutionContext(ExecutionContext delegate, Map<Object, Object> overrides) {
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

        @Override
        public void suspend(String messageRef, Object payload) {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public String getProcessDefinitionId() {
            throw new IllegalStateException("Not supported");
        }

        @Override
        public String getElementId() {
            throw new IllegalStateException("Not supported");
        }
    }
}
