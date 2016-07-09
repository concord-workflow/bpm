package io.takari.bpm.model.diagram;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProcessDiagram implements Serializable {
	
	private static final long serialVersionUID = 1L;

    private final String id;
    private final List<Shape> shapes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    public ProcessDiagram(String id) {
        this.id = id;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<Shape> getShapes() {
        return shapes;
    }

    public String getId() {
        return id;
    }
}
