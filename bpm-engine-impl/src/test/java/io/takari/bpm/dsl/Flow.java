package io.takari.bpm.dsl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Flow implements Serializable {

    private final String id;
    private final List<Step> steps;

    public Flow(String id, List<Step> steps) {
        this.id = id;
        this.steps = steps;
    }

    public Flow(String id, Step ... steps) {
        this(id, Arrays.asList(steps));
    }

    public String getId() {
        return id;
    }

    public List<Step> getSteps() {
        return steps;
    }
}
