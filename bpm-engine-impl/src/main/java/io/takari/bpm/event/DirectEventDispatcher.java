package io.takari.bpm.event;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.api.ExecutionException;

public class DirectEventDispatcher implements EventDispatcher {

    private final AbstractEngine engine;

    public DirectEventDispatcher(AbstractEngine engine) {
        this.engine = engine;
    }

    @Override
    public void dispatch(Event e) throws ExecutionException {
        engine.resume(e, null);
    }
}
