package io.takari.bpm.model;

import java.io.Serializable;
import java.util.Objects;

public class VariableMapping implements Serializable {
	
	private static final long serialVersionUID = 1L;
    
    private final String source;
    private final String sourceExpression;
    private final Object sourceValue;
    private final String target;

    public VariableMapping(String source, String sourceExpression, String target) {
        this(source, sourceExpression, null, target);
    }

    public VariableMapping(String source, String sourceExpression, Object sourceValue, String target) {
        this.source = source;
        this.sourceExpression = sourceExpression;
        this.sourceValue = sourceValue;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public String getSourceExpression() {
        return sourceExpression;
    }

    public Object getSourceValue() {
        return sourceValue;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableMapping that = (VariableMapping) o;

        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        if (sourceExpression != null ? !sourceExpression.equals(that.sourceExpression) : that.sourceExpression != null)
            return false;
        if (sourceValue != null ? !sourceValue.equals(that.sourceValue) : that.sourceValue != null) return false;
        return target.equals(that.target);

    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (sourceExpression != null ? sourceExpression.hashCode() : 0);
        result = 31 * result + (sourceValue != null ? sourceValue.hashCode() : 0);
        result = 31 * result + target.hashCode();
        return result;
    }
}
