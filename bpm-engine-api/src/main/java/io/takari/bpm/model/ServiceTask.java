package io.takari.bpm.model;

import java.util.Set;

public class ServiceTask extends AbstractElement {
	
	private static final long serialVersionUID = 1L;

    /**
     * The variable which contains the result of expression evaluation.
     */
	public static final String EXPRESSION_RESULT_VAR = "__exprResult";
    
    private String name;
    private final String expression;
    private final ExpressionType type;
    private final Set<VariableMapping> in;
    private final Set<VariableMapping> out;
    private final boolean copyAllVariables;

    public ServiceTask(String id) {
        this(id, ExpressionType.NONE, null, false);
    }

    public ServiceTask(String id, ExpressionType type, String expression) {
        this(id, type, expression, null, null, false);
    }

    public ServiceTask(String id, ExpressionType type, String expression, boolean copyAllVariables) {
        this(id, type, expression, null, null, copyAllVariables);
    }

    public ServiceTask(String id, ExpressionType type, String expression, Set<VariableMapping> in, Set<VariableMapping> out) {
        this(id, type, expression, in, out, false);
    }

    public ServiceTask(String id, ExpressionType type, String expression, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables) {
        super(id);
        this.type = type;
        this.expression = expression;
        this.in = in;
        this.out = out;
        this.copyAllVariables = copyAllVariables;
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

    public boolean isCopyAllVariables() {
        return copyAllVariables;
    }

    @Override
    public String toString() {
        return "ServiceTask (" + getId() + ") {" +
                "name='" + name + '\'' +
                ", expression='" + expression + '\'' +
                ", type=" + type +
                ", in=" + in +
                ", out=" + out +
                ", copyAllVariables=" + copyAllVariables +
                '}';
    }
}
