package io.takari.bpm.model;

public class BoundaryEvent extends AbstractElement {
	
	private static final long serialVersionUID = 1L;

    private final String attachedToRef;
    private final String errorRef;
    private final String timeDuration;

    public BoundaryEvent(String id, String attachedToRef, String errorRef) {
        this(id, attachedToRef, errorRef, null);
    }

    public BoundaryEvent(String id, String attachedToRef, String errorRef, String timeDuration) {
        super(id);
        this.attachedToRef = attachedToRef;
        this.errorRef = errorRef;
        this.timeDuration = timeDuration;
    }

    public String getAttachedToRef() {
        return attachedToRef;
    }

    public String getErrorRef() {
        return errorRef;
    }

    public String getTimeDuration() {
        return timeDuration;
    }

    @Override
    public String toString() {
        return "BoundaryEvent (" + getId() + ") {" +
                "attachedToRef='" + attachedToRef + '\'' +
                ", errorRef='" + errorRef + '\'' +
                ", timeDuration='" + timeDuration + '\'' +
                '}';
    }
}
