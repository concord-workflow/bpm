package io.takari.bpm.commands;

import java.util.Map;

public class ResumeElementCommand extends ProcessElementCommand {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> input;

    public ResumeElementCommand(String definitionId, String elementId, Map<String, Object> input) {
        super(definitionId, elementId);

        this.input = input;
    }

    public Map<String, Object> getInput() {
        return input;
    }
}
