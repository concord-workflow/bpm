package io.takari.bpm.actions;

import io.takari.bpm.context.Change;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.IntermediateCatchEvent;

import java.util.Map;

public class CreateEventAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String definitionId;
    private final String elementId;
    private final String messageRef;
    private final String messageRefExpression;
    private final String timeDate;
    private final String timeDuration;
    private final Object payload;
    private final boolean resumeFromSameStep;
    private final Map<String, Change> ctxChangesBeforeSuspend;

    public CreateEventAction(String definitionId, IntermediateCatchEvent ev) {
        this(definitionId, ev.getId(), ev.getMessageRef(), ev.getMessageRefExpression(),
                ev.getTimeDate(), ev.getTimeDuration(), ev.getPayload(), false);
    }

    public CreateEventAction(String definitionId, String elementId) {
        this(definitionId, elementId, null, null, null, null, null, false);
    }

    public CreateEventAction(String definitionId, String elementId, String messageRef,
                             String messageRefExpression, String timeDate, String timeDuration,
                             Object payload, boolean resumeFromSameStep) {
        this(definitionId, elementId, messageRef, messageRefExpression, timeDate, timeDuration, payload, resumeFromSameStep, null);
    }

    public CreateEventAction(String definitionId, String elementId, String messageRef,
                String messageRefExpression, String timeDate, String timeDuration,
                Object payload, boolean resumeFromSameStep, Map<String, Change> ctxChangesBeforeSuspend) {

        this.definitionId = definitionId;
        this.elementId = elementId;
        this.messageRef = messageRef;
        this.messageRefExpression = messageRefExpression;
        this.timeDate = timeDate;
        this.timeDuration = timeDuration;
        this.payload = payload;
        this.resumeFromSameStep = resumeFromSameStep;
        this.ctxChangesBeforeSuspend = ctxChangesBeforeSuspend;
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

    public Object getPayload() {
        return payload;
    }

    public boolean isResumeFromSameStep() {
        return resumeFromSameStep;
    }

    public Map<String, Change> getCtxChangesBeforeSuspend() {
        return ctxChangesBeforeSuspend;
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
                ", payload=" + payload +
                ", resumeFromSameStep=" + resumeFromSameStep +
                ", ctxChangesBeforeSuspend=" + ctxChangesBeforeSuspend +
                ']';
    }
}
