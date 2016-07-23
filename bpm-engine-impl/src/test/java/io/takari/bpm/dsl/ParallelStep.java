package io.takari.bpm.dsl;

import java.util.Arrays;
import java.util.List;

public class ParallelStep implements Step {

    private final List<Step> steps;

    public ParallelStep(List<Step> steps) {
        this.steps = steps;
    }

    public ParallelStep(Step ... steps) {
        this(Arrays.asList(steps));
    }

    public List<Step> getSteps() {
        return steps;
    }
}
