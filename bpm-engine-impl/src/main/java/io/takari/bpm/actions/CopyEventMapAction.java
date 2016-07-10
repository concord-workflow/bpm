package io.takari.bpm.actions;

import io.takari.bpm.state.EventMapHelper.EventMap;

public class CopyEventMapAction implements Action {

    private static final long serialVersionUID = 1L;

    private final EventMap eventMap;

    public CopyEventMapAction(EventMap eventMap) {
        this.eventMap = eventMap;
    }

    public EventMap getEventMap() {
        return eventMap;
    }

    @Override
    public String toString() {
        return "CopyEventMapAction [eventMap=" + eventMap + "]";
    }
}
