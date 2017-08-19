package io.takari.bpm.context;

import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.state.Variables;

public class DefaultExecutionContextFactory implements ExecutionContextFactory<ExecutionContextImpl> {

    private final ExpressionManager expressionManager;

    public DefaultExecutionContextFactory(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
    }

    @Override
    public ExecutionContextImpl create(Variables source) {
        return new ExecutionContextImpl(expressionManager, source);
    }
}
