package io.takari.bpm.el;

import io.takari.bpm.api.ExecutionContext;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;

public class ExecutionContextVariableResolver extends ELResolver {

    private final ExecutionContext executionContext;

    public ExecutionContextVariableResolver(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return Object.class;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return Object.class;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null && property instanceof String k) {
            if (executionContext.hasVariable(k)) {
                context.setPropertyResolved(true);
                return executionContext.getVariable(k);
            }
        }

        return null;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
    }
}
