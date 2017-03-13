package io.takari.bpm;

import java.io.Serializable;

public class Configuration implements Serializable {

    private boolean avoidDefinitionReloadingOnCall = true;
    private UnhandledBpmnErrorStrategy unhandledBpmnErrorStrategy = UnhandledBpmnErrorStrategy.PROPAGATE;
    private boolean storeExpressionEvalResultsInContext = true;

    public boolean isThrowExceptionOnUnhandledBpmnError() {
        return unhandledBpmnErrorStrategy == UnhandledBpmnErrorStrategy.EXCEPTION;
    }

    /**
     * Throw an exception on an unhandled {@code BpmnError} (e.g. when error occurs in a subprocess without a boundary error
     * event).
     *
     * @param throwExceptionOnUnhandledBpmnError
     * @deprecated use {@link #setUnhandledBpmnErrorStrategy(UnhandledBpmnErrorStrategy)}
     */
    @Deprecated
    public void setThrowExceptionOnUnhandledBpmnError(boolean throwExceptionOnUnhandledBpmnError) {
        this.unhandledBpmnErrorStrategy = UnhandledBpmnErrorStrategy.EXCEPTION;
    }

    public UnhandledBpmnErrorStrategy getUnhandledBpmnErrorStrategy() {
        return unhandledBpmnErrorStrategy;
    }

    /**
     * Determines how to process unhandled BPMN errors (e.g. errors in subprocesses without boundary error events).
     * By default, unhandled errors are propagated to a higher level.
     *
     * @param unhandledBpmnErrorStrategy
     */
    public void setUnhandledBpmnErrorStrategy(UnhandledBpmnErrorStrategy unhandledBpmnErrorStrategy) {
        this.unhandledBpmnErrorStrategy = unhandledBpmnErrorStrategy;
    }

    public boolean isAvoidDefinitionReloadingOnCall() {
        return avoidDefinitionReloadingOnCall;
    }

    /**
     * If {@code true}, then a CallActivity's process definition will be loaded only once. Otherwise, it will be
     * reloaded on each call.
     *
     * @param avoidDefinitionReloadingOnCall
     */
    public void setAvoidDefinitionReloadingOnCall(boolean avoidDefinitionReloadingOnCall) {
        this.avoidDefinitionReloadingOnCall = avoidDefinitionReloadingOnCall;
    }

    /**
     * If {@code true}, then results of expression evaluation will be stored in current context.
     *
     * @param storeExpressionEvalResultsInContext
     * @see io.takari.bpm.model.ServiceTask#EXPRESSION_RESULT_VAR
     */
    public void setStoreExpressionEvalResultsInContext(boolean storeExpressionEvalResultsInContext) {
        this.storeExpressionEvalResultsInContext = storeExpressionEvalResultsInContext;
    }

    public boolean isStoreExpressionEvalResultsInContext() {
        return storeExpressionEvalResultsInContext;
    }

    public enum UnhandledBpmnErrorStrategy {

        /**
         * Throw an {@code {@link io.takari.bpm.api.ExecutionException}} immediately.
         */
        EXCEPTION,

        /**
         * Propagate an error to a higher level of a process' stack (similar to Java's exception propagation).
         */
        PROPAGATE,

        /**
         * Exit a subprocess, ignore an error and continue the execution.
         * While this doesn't make much sense, it left for the compatibility with old versions of the engine.
         */
        IGNORE
    }
}
