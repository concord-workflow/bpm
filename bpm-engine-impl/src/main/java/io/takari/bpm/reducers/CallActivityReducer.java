package io.takari.bpm.reducers;

import io.takari.bpm.Configuration;
import io.takari.bpm.actions.SetVariableAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.state.Definitions;
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

import static io.takari.bpm.api.ExecutionContext.CURRENT_FLOW_NAME_KEY;

public class CallActivityReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(CallActivityReducer.class);

    private final ExecutionContextFactory<?> contextFactory;
    private final IndexedProcessDefinitionProvider definitionProvider;
    private final Configuration cfg;

    public CallActivityReducer(ExecutionContextFactory<?> contextFactory, IndexedProcessDefinitionProvider definitionProvider, Configuration cfg) {
        this.contextFactory = contextFactory;
        this.definitionProvider = definitionProvider;
        this.cfg = cfg;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof FindAndCallActivityAction)) {
            return state;
        }

        FindAndCallActivityAction a = (FindAndCallActivityAction) action;
        String proc = resolveCalledElement(state, a);

        // find a called process' definition
        IndexedProcessDefinition sub = null;
        if (cfg.isAvoidDefinitionReloadingOnCall()) {
            Definitions defs = state.getDefinitions();
            sub = defs.get(proc);
        }
        if (sub == null) {
            sub = definitionProvider.getById(proc);
        }

        // add the found definition to the process state
        state = state.setDefinitions(state.getDefinitions().put(sub));

        Object currentFlowName = state.getVariables().getVariable(ExecutionContext.CURRENT_FLOW_NAME_KEY);

        state = state.setVariables(state.getVariables().setVariable(CURRENT_FLOW_NAME_KEY, sub.getId()));

        log.debug("reduce ['{}'] -> new child process '{}'", state.getBusinessKey(), sub.getId());

        // restore flow name variable
        CommandStack stack = state.getStack()
                .push(new PerformActionsCommand(new SetVariableAction(ExecutionContext.CURRENT_FLOW_NAME_KEY, currentFlowName)));
        state = state.setStack(stack);

        // push the start event of the child process to the stack
        StartEvent ev = ProcessDefinitionUtils.findStartEvent(sub);
        state = state.setStack(state.getStack().push(new ProcessElementCommand(sub.getId(), ev.getId())));

        return state;
    }

    private String resolveCalledElement(ProcessInstance state, FindAndCallActivityAction a) {
        if (a.getCalledElementExpression() == null) {
            return a.getCalledElement();
        }

        ExecutionContext ctx = contextFactory.create(state.getVariables());
        return ctx.eval(a.getCalledElementExpression(), String.class);
    }
}
