package io.takari.bpm;

import java.io.Serializable;

public class Configuration implements Serializable {

    private boolean throwExceptionOnErrorEnd = false;
    private boolean throwExceptionOnUnhandledBpmnError = true;

    public boolean isThrowExceptionOnErrorEnd() {
        return throwExceptionOnErrorEnd;
    }

    /**
     * Throw an exception if a process ends with an error end event.
     * @param throwExceptionOnErrorEnd
     */
    public void setThrowExceptionOnErrorEnd(boolean throwExceptionOnErrorEnd) {
        this.throwExceptionOnErrorEnd = throwExceptionOnErrorEnd;
    }

    public boolean isThrowExceptionOnUnhandledBpmnError() {
        return throwExceptionOnUnhandledBpmnError;
    }

    /**
     * Throw an exception on an unhandled {@BpmnError} (e.g. when error occurs in a subprocess without a boundary error
     * event).
     * @param throwExceptionOnUnhandledBpmnError
     */
    public void setThrowExceptionOnUnhandledBpmnError(boolean throwExceptionOnUnhandledBpmnError) {
        this.throwExceptionOnUnhandledBpmnError = throwExceptionOnUnhandledBpmnError;
    }
}
