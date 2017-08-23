package io.takari.bpm.task;

import io.takari.bpm.api.ExecutionContext;

public interface JavaDelegateHandler {

    void execute(Object task, ExecutionContext ctx) throws Exception;
}
