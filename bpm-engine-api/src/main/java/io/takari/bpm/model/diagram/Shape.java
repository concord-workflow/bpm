package io.takari.bpm.model.diagram;

import java.io.Serializable;

public class Shape implements Serializable {
	
	private static final long serialVersionUID = 1L;

    private final String id;
    private final String elementId;
    private final Bounds bounds;
    
    public Shape(String id, String elementId, Bounds bounds) {
        this.id = id;
        this.elementId = elementId;
        this.bounds = bounds;
    }

    public String getElementId() {
        return elementId;
    }

    public String getId() {
        return id;
    }

    public Bounds getBounds() {
        return bounds;
    }
}
