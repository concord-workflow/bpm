package io.takari.bpm.commands;

import io.takari.bpm.misc.CoverageIgnore;

public class ActivityFinalizerCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;

    public ActivityFinalizerCommand(String definitionId, String elementId) {
        this.definitionId = definitionId;
        this.elementId = elementId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "ActivityFinalizerCommand[" +
                "definitionId='" + definitionId + '\'' +
                ", elementId='" + elementId + '\'' +
                ']';
    }
}
