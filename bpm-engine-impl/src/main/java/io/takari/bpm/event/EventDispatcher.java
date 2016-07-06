package io.takari.bpm.event;

import io.takari.bpm.api.ExecutionException;

public interface EventDispatcher {

    void dispatch(Event e) throws ExecutionException;
}
