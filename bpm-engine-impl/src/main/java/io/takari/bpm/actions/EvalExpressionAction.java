package io.takari.bpm.actions;

import io.takari.bpm.commands.Command;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.ExpressionType;
import io.takari.bpm.utils.Timeout;

import java.util.List;
import java.util.Map;

public class EvalExpressionAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final ExpressionType type;
    private final String expression;
    private final Command defaultCommand;
    private final List<Timeout<Command>> timeouts;
    private final Command defaultError;
    private final Map<String, Command> errors;

    private EvalExpressionAction(String definitionId, String elementId, ExpressionType type, String expression,
                                 Command defaultCommand, List<Timeout<Command>> timeouts, Command defaultError, Map<String, Command> errors) {
        this.definitionId = definitionId;
        this.elementId = elementId;
        this.type = type;
        this.expression = expression;
        this.defaultCommand = defaultCommand;
        this.timeouts = timeouts;
        this.defaultError = defaultError;
        this.errors = errors;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public ExpressionType getType() {
        return type;
    }

    public String getExpression() {
        return expression;
    }

    public Command getDefaultCommand() {
        return defaultCommand;
    }

    public List<Timeout<Command>> getTimeouts() {
        return timeouts;
    }

    public Command getDefaultError() {
        return defaultError;
    }

    public Map<String, Command> getErrors() {
        return errors;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "EvalExpressionAction [definitionId=" + definitionId + ", elementId=" + elementId + ", type=" + type + ", expression="
                + expression + ", defaultCommand=" + defaultCommand + ", timeouts=" + timeouts + ", defaultError=" + defaultError
                + ", errors=" + errors + "]";
    }

    public static class Builder {

        private final String definitionId;
        private final String elementId;
        private final ExpressionType type;
        private final String expression;
        private final Command defaultCommand;

        private List<Timeout<Command>> timeouts;
        private Command defaultError;
        private Map<String, Command> errors;

        public Builder(String definitionId, String elementId, ExpressionType type, String expression, Command defaultCommand) {
            this.definitionId = definitionId;
            this.elementId = elementId;
            this.type = type;
            this.expression = expression;
            this.defaultCommand = defaultCommand;
        }

        public Builder withTimeouts(List<Timeout<Command>> timeouts) {
            this.timeouts = timeouts;
            return this;
        }

        public Builder withDefaultError(Command defaultError) {
            this.defaultError = defaultError;
            return this;
        }

        public Builder withErrors(Map<String, Command> errors) {
            this.errors = errors;
            return this;
        }

        public EvalExpressionAction build() {
            return new EvalExpressionAction(definitionId, elementId, type, expression, defaultCommand, timeouts, defaultError, errors);
        }
    }
}
