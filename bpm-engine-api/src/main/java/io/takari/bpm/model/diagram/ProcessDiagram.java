package io.takari.bpm.model.diagram;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ProcessDiagram implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final List<Shape> shapes;
    private final List<Edge> edges;

    public ProcessDiagram(String id) {
        this(id, Collections.emptyList(), Collections.emptyList());
    }

    public ProcessDiagram(String id, List<Shape> shapes, List<Edge> edges) {
        this.id = id;
        this.shapes = shapes;
        this.edges = edges;
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
