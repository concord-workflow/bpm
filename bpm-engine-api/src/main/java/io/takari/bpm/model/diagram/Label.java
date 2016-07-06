package io.takari.bpm.model.diagram;

import java.io.Serializable;

public class Label implements Serializable {

    private final Bounds bounds;

    public Label(Bounds bounds) {
        this.bounds = bounds;
    }

    public Bounds getBounds() {
        return bounds;
    }
}
