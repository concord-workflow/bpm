package io.takari.bpm.actions;

import io.takari.bpm.commands.Command;
import io.takari.bpm.misc.CoverageIgnore;

import java.util.Map;

public class ExecuteScriptAction implements Action {

    private final String definitionId;
    private final String elementId;
    private Command defaultError;
    private final Map<String, Command> errors;

    public ExecuteScriptAction(String definitionId, String elementId, Command defaultError, Map<String, Command> errors) {
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.defaultError = defaultError;
        this.errors = errors;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public Map<String, Command> getErrors() {
        return errors;
    }

    public Command getDefaultError() {
        return defaultError;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "ExecuteScriptAction{" +
                "definitionId='" + definitionId + '\'' +
                ", elementId='" + elementId + '\'' +
                ", defaultError=" + defaultError +
                ", errors=" + errors +
                '}';
    }
}
