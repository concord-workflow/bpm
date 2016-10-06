package io.takari.bpm.reducers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.IndexedProcessDefinitionProvider;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FindAndCallActivityAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.state.ProcessInstance;

public class CallActivityReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(CallActivityReducer.class);

    private final IndexedProcessDefinitionProvider definitionProvider;

    public CallActivityReducer(IndexedProcessDefinitionProvider definitionProvider) {
        this.definitionProvider = definitionProvider;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof FindAndCallActivityAction)) {
            return state;
        }

        FindAndCallActivityAction a = (FindAndCallActivityAction) action;

        // find a called process' definition
        IndexedProcessDefinition sub = definitionProvider.getById(a.getCalledElement());

        // add the found definition to the process state
        state = state.setDefinitions(state.getDefinitions().put(sub));

        log.debug("reduce ['{}'] -> new child process '{}'", state.getBusinessKey(), sub.getId());

        // push the start event of the child process to the stack
        StartEvent ev = ProcessDefinitionUtils.findStartEvent(sub);
        state = state.setStack(state.getStack().push(new ProcessElementCommand(sub.getId(), ev.getId()/*, a.getScopeId(), false*/)));

        return state;
    }
}
