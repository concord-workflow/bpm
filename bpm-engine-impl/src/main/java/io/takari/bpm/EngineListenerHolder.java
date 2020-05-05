package io.takari.bpm;

import io.takari.bpm.state.ProcessInstance;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EngineListenerHolder {

    private final List<EngineListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(EngineListener listener) {
        this.listeners.add(listener);
    }

    public ProcessInstance fireOnFinalize(ProcessInstance state) {
        for (EngineListener l : listeners) {
            state = l.onFinalize(state);
        }
        return state;
    }

    public void fireOnUnhandledException(ProcessInstance state) {
        for (EngineListener l : listeners) {
            l.onUnhandledException(state);
        }
    }
}
