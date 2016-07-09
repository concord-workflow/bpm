package io.takari.bpm.model.diagram;

import java.io.Serializable;

public class Waypoint implements Serializable {
	
	private static final long serialVersionUID = 1L;

    private final double x;
    private final double y;

    public Waypoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
