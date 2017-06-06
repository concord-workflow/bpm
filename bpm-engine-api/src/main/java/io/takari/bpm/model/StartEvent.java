package io.takari.bpm.model;

public class StartEvent extends AbstractElement {
	
	private static final long serialVersionUID = 1L;

    public StartEvent(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return "StartEvent (" + getId() + ")";
    }
}
