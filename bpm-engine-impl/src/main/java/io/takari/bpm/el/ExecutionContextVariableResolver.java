package io.takari.bpm.el;

import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.misc.CoverageIgnore;

import javax.el.ELContext;
import javax.el.ELResolver;
import java.beans.FeatureDescriptor;
import java.util.Iterator;

public class ExecutionContextVariableResolver extends ELResolver {

    private final ExecutionContext executionContext;

    public ExecutionContextVariableResolver(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    @CoverageIgnore
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return Object.class;
    }

    @Override
    @CoverageIgnore
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    @CoverageIgnore
    public Class<?> getType(ELContext context, Object base, Object property) {
        return Object.class;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null && property instanceof String) {
            String k = (String) property;
            if (executionContext.hasVariable(k)) {
                context.setPropertyResolved(true);
                return executionContext.getVariable(k);
            }
        }

        return null;
    }

    @Override
    @CoverageIgnore
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return true;
    }

    @Override
    @CoverageIgnore
    public void setValue(ELContext context, Object base, Object property, Object value) {
    }
}
