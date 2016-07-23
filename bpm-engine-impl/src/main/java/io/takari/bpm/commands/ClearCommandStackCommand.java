package io.takari.bpm.commands;

public class ClearCommandStackCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final String definitionId;

    public ClearCommandStackCommand(String definitionId) {
        this.definitionId = definitionId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    @Override
    public String toString() {
        return "ClearCommandStackCommand [definitionId=" + definitionId + "]";
    }
}
