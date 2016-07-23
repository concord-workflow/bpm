package io.takari.bpm.dsl;

public class TaskStep implements Step {

    private final String task;

    public TaskStep(String task) {
        this.task = task;
    }

    public String getTask() {
        return task;
    }
}
