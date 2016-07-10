package io.takari.bpm.commands;

public class ProcessEventMappingCommand implements Command {

    private static final long serialVersionUID = 1L;

    private final String definitionId;

    public ProcessEventMappingCommand(String definitionId) {
        this.definitionId = definitionId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    @Override
    public String toString() {
        return "ProcessEventMappingCommand [definitionId=" + definitionId + "]";
    }
}
