package io.takari.bpm.model;

import java.util.Set;

public class ServiceTask extends AbstractElement {
	
	private static final long serialVersionUID = 1L;
    
    private String name;
    private final String expression;
    private final ExpressionType type;
    private final Set<VariableMapping> in;
    private final Set<VariableMapping> out;

    public ServiceTask(String id) {
        this(id, ExpressionType.NONE, null);
    }

    public ServiceTask(String id, ExpressionType type, String expression) {
        this(id, type, expression, null, null);
    }

    public ServiceTask(String id, ExpressionType type, String expression, Set<VariableMapping> in, Set<VariableMapping> out) {
        super(id);
        this.type = type;
        this.expression = expression;
        this.in = in;
        this.out = out;
    }

    public String getExpression() {
        return expression;
    }

    public ExpressionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<VariableMapping> getIn() {
        return in;
    }

    public Set<VariableMapping> getOut() {
        return out;
    }
}
