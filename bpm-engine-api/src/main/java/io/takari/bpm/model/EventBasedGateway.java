package io.takari.bpm.model;

public class EventBasedGateway extends AbstractElement {
	
	private static final long serialVersionUID = 1L;

    public EventBasedGateway(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return "EventBasedGateway (" + getId() + ")";
    }
}
