package io.takari.bpm.model;

public class ExclusiveGateway extends AbstractElement {
	
	private static final long serialVersionUID = 1L;

    private final String defaultFlow;

    public ExclusiveGateway(String id) {
        this(id, null);
    }

    public ExclusiveGateway(String id, String defaultFlow) {
        super(id);
        this.defaultFlow = defaultFlow;
    }

    public String getDefaultFlow() {
        return defaultFlow;
    }

    @Override
    public String toString() {
        return "ExclusiveGateway (" + getId() + ") {" +
                "defaultFlow='" + defaultFlow + '\'' +
                '}';
    }
}
