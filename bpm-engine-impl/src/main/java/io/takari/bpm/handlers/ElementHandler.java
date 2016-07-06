package io.takari.bpm.handlers;

import io.takari.bpm.DefaultExecution;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;

public interface ElementHandler {

    void handle(DefaultExecution s, ProcessElementCommand c) throws ExecutionException;
}
