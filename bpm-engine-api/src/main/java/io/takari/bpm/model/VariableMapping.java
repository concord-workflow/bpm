package io.takari.bpm.model;

import java.io.Serializable;

public class VariableMapping implements Serializable {

    public static VariableMapping copy(String source, String target) {
        return new VariableMapping(source, null, null, target, false);
    }

    public static VariableMapping set(Object value, String target) {
        return new VariableMapping(null, null, value, target, false);
    }

    public static VariableMapping eval(String expr, String target) {
        return new VariableMapping(null, expr, null, target, false);
    }

    private static final long serialVersionUID = 1L;

    private final String source;
    private final String sourceExpression;
    private final Object sourceValue;
    private final String target;
    private final boolean interpolateValue;

    public VariableMapping(String source, String sourceExpression, String target) {
        this(source, sourceExpression, null, target);
    }

    public VariableMapping(String source, String sourceExpression, Object sourceValue, String target) {
        this(source, sourceExpression, sourceValue, target, false);
    }

    public VariableMapping(String source, String sourceExpression, Object sourceValue, String target, boolean interpolateValue) {
        this.source = source;
        this.sourceExpression = sourceExpression;
        this.sourceValue = sourceValue;
        this.target = target;
        this.interpolateValue = interpolateValue;
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

    public boolean isInterpolateValue() {
        return interpolateValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableMapping that = (VariableMapping) o;

        if (interpolateValue != that.interpolateValue) return false;
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
        result = 31 * result + (interpolateValue ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "VariableMapping [" +
                "source=" + source +
                ", sourceExpression=" + sourceExpression +
                ", sourceValue=" + sourceValue +
                ", target=" + target +
                ", interpolateValue=" + interpolateValue +
                ']';
    }
}
