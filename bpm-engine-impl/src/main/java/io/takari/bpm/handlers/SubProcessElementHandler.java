package io.takari.bpm.handlers;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.ExecutionContextImpl;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.HandleRaisedErrorCommand;
import io.takari.bpm.commands.MergeExecutionContextCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.VariableMapping;
import java.util.Set;

public class SubProcessElementHandler extends AbstractCallHandler {

    public SubProcessElementHandler(AbstractEngine engine) {
        super(engine);
    }

    @Override
    public void handle(DefaultExecution s, ProcessElementCommand c) throws ExecutionException {
        s.pop();

        ProcessDefinition sub = findCalledProcess(c);

        // add an error handling command to the stack
        s.push(new HandleRaisedErrorCommand(c));

        // get the ID of the called process. Depending on the call type
        // ('sub-process' or 'call activity') it can be:
        // - ID of process, which contains the element of calling process;
        // - ID of external process from a separate process definition
        String id = getCalledProcessId(c, sub);

        // push the first command to the called process' stack
        AbstractElement start = ProcessDefinitionUtils.findStartEvent(sub);
        s.push(new ProcessElementCommand(id, start.getId()));
    }

    @Override
    protected ProcessDefinition findCalledProcess(ProcessElementCommand c) throws ExecutionException {
        ProcessDefinition pd = getProcessDefinition(c);
        return ProcessDefinitionUtils.findSubProcess(pd, c.getElementId());
    }

    @Override
    protected String getCalledProcessId(ProcessElementCommand c, ProcessDefinition sub) throws ExecutionException {
        return c.getProcessDefinitionId();
    }

    @Override
    protected MergeExecutionContextCommand makeMergeCommand(ExecutionContext parent, ExecutionContext child, Set<VariableMapping> outVariables) {
        return new MergeExecutionContextCommand(parent);
    }

    @Override
    protected ExecutionContext makeChildContext(DefaultExecution s, ProcessElementCommand c) throws ExecutionException {
        return new ExecutionContextImpl(s.getContext());
    }
}
