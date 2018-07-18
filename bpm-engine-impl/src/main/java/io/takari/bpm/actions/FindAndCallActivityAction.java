package io.takari.bpm.actions;

import io.takari.bpm.misc.CoverageIgnore;

public class FindAndCallActivityAction implements Action {

    private static final long serialVersionUID = 1L;

    private final String calledElement;

    private final String calledElementExpression;

    public FindAndCallActivityAction(String calledElement) {
        this(calledElement, null);
    }

    public FindAndCallActivityAction(String calledElement, String calledElementExpression) {
        this.calledElement = calledElement;
        this.calledElementExpression = calledElementExpression;
    }

    public String getCalledElement() {
        return calledElement;
    }

    public String getCalledElementExpression() {
        return calledElementExpression;
    }

    @Override
    @CoverageIgnore
    public String toString() {
        return "FindAndCallActivityAction [" +
                "calledElement=" + calledElement + ',' +
                "calledElementExpression=" + calledElementExpression +
                ']';
    }
}
