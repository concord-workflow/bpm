package io.takari.bpm.commands;

import io.takari.bpm.context.Change;

import java.util.Map;

public class ResumeElementCommand extends ProcessElementCommand {

    private static final long serialVersionUID = 1L;

    private final Map<String, Change> ctxChangesBeforeSuspend;

    public ResumeElementCommand(String definitionId, String elementId, Map<String, Change> ctxChangesBeforeSuspend) {
        super(definitionId, elementId);

        this.ctxChangesBeforeSuspend = ctxChangesBeforeSuspend;
    }

    public Map<String, Change> getCtxChangesBeforeSuspend() {
        return ctxChangesBeforeSuspend;
    }
}
