package io.takari.bpm.handlers;

import io.takari.bpm.AbstractEngine;
import io.takari.bpm.DefaultExecution;
import io.takari.bpm.EventMapHelper;
import io.takari.bpm.ExecutionContextImpl;
import io.takari.bpm.IndexedProcessDefinitionProvider;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.MergeExecutionContextCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.CallActivity;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.VariableMapping;
import java.util.HashSet;
import java.util.Set;

public class CallActivityElementHandler extends AbstractCallHandler {

    public CallActivityElementHandler(AbstractEngine engine) {
        super(engine);
    }

    @Override
    protected ProcessDefinition findCalledProcess(ProcessElementCommand c) throws ExecutionException {
        ProcessDefinition pd = getProcessDefinition(c);
        CallActivity act = (CallActivity) ProcessDefinitionUtils.findElement(pd, c.getElementId());

        IndexedProcessDefinitionProvider provider = getEngine().getProcessDefinitionProvider();
        return provider.getById(act.getCalledElement());
    }

    @Override
    protected String getCalledProcessId(ProcessElementCommand c, ProcessDefinition sub) throws ExecutionException {
        return sub.getId();
    }

    @Override
    protected MergeExecutionContextCommand makeMergeCommand(ExecutionContext parent, ExecutionContext child, Set<VariableMapping> outVariables) {
        // we need to pass an updated events map to the parent process (from the
        // callee back to the caller)
        Set<VariableMapping> out = new HashSet<>();
        if (outVariables != null) {
            out.addAll(outVariables);
        }
        EventMapHelper.addOutMapping(out);

        return new MergeExecutionContextCommand(parent, out);
    }

    @Override
    protected ExecutionContext makeChildContext(DefaultExecution s) {
        // we need to pass an events map to the called process
        ExecutionContext ctx = new ExecutionContextImpl(null);
        EventMapHelper.link(s.getContext(), ctx);

        return ctx;
    }
}
