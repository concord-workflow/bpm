package io.takari.bpm.model;

public class IntermediateCatchEvent extends AbstractElement {

    public static IntermediateCatchEvent message(String id, String messageRef) {
        return new IntermediateCatchEvent(id, messageRef, null, null, null, null);
    }

    public static IntermediateCatchEvent messageExpr(String id, String messageRefExpression) {
        return new IntermediateCatchEvent(id, null, messageRefExpression, null, null, null);
    }

    public static IntermediateCatchEvent timeDate(String id, String timeDate) {
        return new IntermediateCatchEvent(id, null, null, timeDate, null, null);
    }

    public static IntermediateCatchEvent timeDuration(String id, String timeDuration) {
        return new IntermediateCatchEvent(id, null, null, null, timeDuration, null);
    }
	
	private static final long serialVersionUID = 1L;

    private final String messageRef;
    private final String messageRefExpression;
    private final String timeDate;
    private final String timeDuration;
    private final Object payload;

    public IntermediateCatchEvent(String id) {
        this(id, null, null, null);
    }
    
    public IntermediateCatchEvent(String id, String messageRef) {
        this(id, messageRef, null, null);
    }

    public IntermediateCatchEvent(String id, String messageRef, Object payload) {
        this(id, messageRef, null, null, null, payload);
    }

    public IntermediateCatchEvent(String id, String messageRef, String timeDate, String timeDuration) {
        this(id, messageRef, null, timeDate, timeDuration, null);
    }

    public IntermediateCatchEvent(String id, String messageRef, String messageRefExpression, String timeDate, String timeDuration,
                                  Object payload) {
        super(id);
        this.messageRef = messageRef;
        this.messageRefExpression = messageRefExpression;
        this.timeDate = timeDate;
        this.timeDuration = timeDuration;
        this.payload = payload;
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

    @Override
    public String toString() {
        return "IntermediateCatchEvent (" + getId() + ") {" +
                "messageRef='" + messageRef + '\'' +
                ", messageRefExpression='" + messageRefExpression + '\'' +
                ", timeDate='" + timeDate + '\'' +
                ", timeDuration='" + timeDuration + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
