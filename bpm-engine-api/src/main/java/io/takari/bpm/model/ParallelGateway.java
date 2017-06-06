package io.takari.bpm.model;

public class ParallelGateway extends AbstractElement {
	
	private static final long serialVersionUID = 1L;
    
    public ParallelGateway(String id) {
        super(id);
    }

    @Override
    public String toString() {
        return "ParallelGateway (" + getId() + ")";
    }
}
