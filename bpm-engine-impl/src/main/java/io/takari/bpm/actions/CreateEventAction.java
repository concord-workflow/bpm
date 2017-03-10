package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.IntermediateCatchEvent;

public class CreateEventAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final String messageRef;
    private final String messageRefExpression;
    private final String timeDate;
    private final String timeDuration;

    public CreateEventAction(String definitionId, IntermediateCatchEvent ev) {
        this(definitionId, ev.getId(), ev.getMessageRef(), ev.getMessageRefExpression(),
                ev.getTimeDate(), ev.getTimeDuration());
    }

    public CreateEventAction(String definitionId, String elementId) {
        this(definitionId, elementId, null, null, null, null);
    }

    public CreateEventAction(String definitionId, String elementId, String messageRef,
                             String messageRefExpression, String timeDate, String timeDuration) {

        this.definitionId = definitionId;
        this.elementId = elementId;
        this.messageRef = messageRef;
        this.messageRefExpression = messageRefExpression;
        this.timeDate = timeDate;
        this.timeDuration = timeDuration;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getElementId() {
        return elementId;
    }

    public String getMessageRef() {
        return messageRef;
    }

    public String getMessageRefExpression() {
        return messageRefExpression;
    }

    public String getTimeDate() {
        return timeDate;
    }

    public String getTimeDuration() {
        return timeDuration;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "CreateEventAction [" +
                "definitionId=" + definitionId +
                ", elementId=" + elementId +
                ", messageRef=" + messageRef +
                ", messageRefExpression=" + messageRefExpression +
                ", timeDate=" + timeDate +
                ", timeDuration=" + timeDuration +
                ']';
    }
}
