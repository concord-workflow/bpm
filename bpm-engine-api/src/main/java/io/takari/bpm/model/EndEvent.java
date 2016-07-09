package io.takari.bpm.model;

public class EndEvent extends AbstractElement {
	
	private static final long serialVersionUID = 1L;

    private final String errorRef;

    public EndEvent(String id) {
        this(id, null);
    }
    
    public EndEvent(String id, String errorRef) {
        super(id);
        this.errorRef = errorRef;
    }

    public String getErrorRef() {
        return errorRef;
    }
}
