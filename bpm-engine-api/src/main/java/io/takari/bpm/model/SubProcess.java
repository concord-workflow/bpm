package io.takari.bpm.model;

import java.util.Collection;

public class SubProcess extends ProcessDefinition {
	
	private static final long serialVersionUID = 1L;
    
    private String name;
    
    public SubProcess(String id, Collection<AbstractElement> children) {
        super(id, children);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
