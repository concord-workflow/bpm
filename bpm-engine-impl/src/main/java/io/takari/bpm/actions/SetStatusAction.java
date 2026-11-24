package io.takari.bpm.actions;

import io.takari.bpm.state.ProcessStatus;

public class SetStatusAction implements Action {

    private static final long serialVersionUID = 1L;

    private final ProcessStatus status;

    public SetStatusAction(ProcessStatus status) {
        this.status = status;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "SetStatusAction [status=" + status + "]";
    }
}
