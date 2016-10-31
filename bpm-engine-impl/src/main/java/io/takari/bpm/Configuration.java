package io.takari.bpm;

import java.io.Serializable;

public class Configuration implements Serializable {

    private boolean throwExceptionOnErrorEnd = false;
    private boolean throwExceptionOnUnhandledBpmnError = true;
    private boolean avoidDefinitionReloadingOnCall = true;

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

    public boolean isAvoidDefinitionReloadingOnCall() {
        return avoidDefinitionReloadingOnCall;
    }

    /**
     * If {@code true}, then a CallActivity's process definition will be loaded only once. Otherwise, it will be
     * reloaded on each call.
     * @param avoidDefinitionReloadingOnCall
     */
    public void setAvoidDefinitionReloadingOnCall(boolean avoidDefinitionReloadingOnCall) {
        this.avoidDefinitionReloadingOnCall = avoidDefinitionReloadingOnCall;
    }
}
