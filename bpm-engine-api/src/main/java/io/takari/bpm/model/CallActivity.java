package io.takari.bpm.model;

import java.util.Set;

public class CallActivity extends AbstractElement {
	
	private static final long serialVersionUID = 1L;

    private String name;
    private final String calledElement;
    private final Set<VariableMapping> in;
    private final Set<VariableMapping> out;
    private final boolean copyAllVariables;
    
    public CallActivity(String id, String calledElement) {
        this(id, calledElement, null, null, false);
    }

    public CallActivity(String id, String calledElement, boolean copyAllVariables) {
        this(id, calledElement, null, null, copyAllVariables);
    }

    public CallActivity(String id, String calledElement, Set<VariableMapping> in, Set<VariableMapping> out) {
        this(id, calledElement, in, out, false);
    }

    public CallActivity(String id, String calledElement, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables) {
        super(id);
        this.calledElement = calledElement;
        this.in = in;
        this.out = out;
        this.copyAllVariables = copyAllVariables;
    }

    public String getCalledElement() {
        return calledElement;
    }

    public Set<VariableMapping> getIn() {
        return in;
    }

    public Set<VariableMapping> getOut() {
        return out;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCopyAllVariables() {
        return copyAllVariables;
    }

    @Override
    public String toString() {
        return "CallActivity (" + getId() + ") {" +
                "name='" + name + '\'' +
                ", calledElement='" + calledElement + '\'' +
                ", in=" + in +
                ", out=" + out +
                ", copyAllVariables=" + copyAllVariables +
                '}';
    }
}
