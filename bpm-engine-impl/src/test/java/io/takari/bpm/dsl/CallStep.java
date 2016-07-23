package io.takari.bpm.dsl;

public class CallStep implements Step {

    private final String call;

    public CallStep(String call) {
        this.call = call;
    }

    public String getCall() {
        return call;
    }
}
