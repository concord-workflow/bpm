package io.takari.bpm.elements;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.*;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.*;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.model.SubProcess;
import io.takari.bpm.model.VariableMapping;
import io.takari.bpm.state.ProcessInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SubProcessHandler implements ElementHandler {

    @Override
    public List<Action> handle(ProcessInstance state, ProcessElementCommand cmd, List<Action> actions) throws ExecutionException {
        actions.add(new PopCommandAction());

        IndexedProcessDefinition pd = state.getDefinition(cmd.getDefinitionId());
        SubProcess s = (SubProcess) ProcessDefinitionUtils.findElement(pd, cmd.getElementId());

        // add an error handling command to the stack
        Command errorHandlingCmd = new ActivityFinalizerCommand(cmd.getDefinitionId(), cmd.getElementId());
        actions.add(new PushCommandAction(errorHandlingCmd));

        List<Command> finishers = new ArrayList<>();
        finishers.add(errorHandlingCmd);

        if (s.isUseSeparateContext()) {
            // set a new variables container (aka child's "ExecutionContext") as our current
            actions.add(new MakeSubProcessVariablesAction(true));

            Set<VariableMapping> outVariables = s.getOutVariables();
            if (outVariables == null) {
                outVariables = Collections.emptySet();
            }

            // restore the original variables
            Command mergeCommand = new MergeVariablesCommand(state.getVariables(), outVariables);
            actions.add(new PushCommandAction(mergeCommand));
            finishers.add(mergeCommand);
        }

        // find an start event of the subprocess
        ProcessDefinition sub = ProcessDefinitionUtils.findSubProcess(pd, cmd.getElementId());
        StartEvent start = ProcessDefinitionUtils.findStartEvent(sub);

        // add a start command to the stack
        Command startCmd = new ProcessElementCommand(cmd.getDefinitionId(), start.getId()/*, scopeId, false*/);
        actions.add(new PushCommandAction(startCmd));

        // push the new scope when the subprocess execution begins
        // the scope will be "popped" by one of the finishing actions
        actions.add(new PushCommandAction(new PerformActionsCommand(new PushScopeAction(false, finishers.toArray(new Command[finishers.size()])))));

        return actions;
    }

}
