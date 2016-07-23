package io.takari.bpm.utils;

import java.io.Serializable;

public class Timeout<T> implements Serializable {

    private final long duration;
    private final T payload;

    public Timeout(long duration, T payload) {
        this.duration = duration;
        this.payload = payload;
    }

    public long getDuration() {
        return duration;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Timeout [duration=" + duration + ", payload=" + payload + "]";
    }
}
