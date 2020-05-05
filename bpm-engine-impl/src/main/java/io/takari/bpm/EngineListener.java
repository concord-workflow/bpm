package io.takari.bpm;

import io.takari.bpm.state.ProcessInstance;

public interface EngineListener {

    ProcessInstance onFinalize(ProcessInstance state);

    default void onUnhandledException(ProcessInstance state) {
    }
}
