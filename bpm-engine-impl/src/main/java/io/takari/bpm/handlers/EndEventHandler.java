package io.takari.bpm.handlers;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.BpmnErrorHelper;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ProcessDefinition;

public class EndEventHandler extends AbstractElementHandler {

    public EndEventHandler(AbstractEngine engine) {
        super(engine);
    }

    @Override
    public void handle(DefaultExecution s, ProcessElementCommand c) throws ExecutionException {
        s.pop();

        ProcessDefinition pd = getProcessDefinition(c);
        EndEvent e = (EndEvent) ProcessDefinitionUtils.findElement(pd, c.getElementId());

        if (e.getErrorRef() != null) {
            // the element has an error ref - will raise an error to the parent
            // process.
            BpmnErrorHelper.raiseError(s.getContext(), e.getErrorRef());
        }
    }
}
