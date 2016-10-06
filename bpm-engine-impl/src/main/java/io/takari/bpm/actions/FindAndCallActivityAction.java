package io.takari.bpm.actions;

public class FindAndCallActivityAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String calledElement;

    public FindAndCallActivityAction(String calledElement) {
        this.calledElement = calledElement;
    }

    public String getCalledElement() {
        return calledElement;
    }

    @Override
    public String toString() {
        return "FindAndCallActivityAction[" +
                "calledElement='" + calledElement + '\'' +
                ']';
    }
}
