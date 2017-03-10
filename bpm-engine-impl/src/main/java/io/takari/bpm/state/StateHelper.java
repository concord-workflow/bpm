package io.takari.bpm.state;

import io.takari.bpm.Executor;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.FireOnStartInterceptorsAction;
import io.takari.bpm.actions.PopScopeAction;
import io.takari.bpm.actions.PushScopeAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.state.Activations.ActivationKey;
import io.takari.bpm.state.Events.EventRecord;
import io.takari.bpm.state.Scopes.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class StateHelper {

    private static final Logger log = LoggerFactory.getLogger(StateHelper.class);
    private static final boolean PRINT_ACTIVATIONS = false;

    public static ProcessInstance createInitialState(Executor executor, UUID id, String businessKey,
                                                     IndexedProcessDefinition pd, Map<String, Object> args) throws ExecutionException {

        ProcessInstance state = new ProcessInstance(id, businessKey, pd);
        StartEvent start = ProcessDefinitionUtils.findStartEvent(pd);

        CommandStack stack = state.getStack();

        // initial scope removal
        stack = stack.push(new PerformActionsCommand(new PopScopeAction()));

        // add the first process command to the stack
        stack = stack.push(new ProcessElementCommand(pd.getId(), start.getId()));

        // initial scope creation
        stack = stack.push(new PerformActionsCommand(new PushScopeAction(pd.getId(), start.getId(), false)));

        state = state.setStack(stack);

        // set external variables
        state = applyVariables(state, pd.getAttributes(), args);

        // fire interceptors
        // TODO add to stack?
        Action a = new FireOnStartInterceptorsAction(pd.getId());
        state = executor.eval(state, Collections.singletonList(a));

        return state;
    }

    public static ProcessInstance applyVariables(ProcessInstance state, Map<String, String> attr, Map<String, Object> args) {
        Variables vars = state.getVariables();

        if (attr != null) {
            for (Map.Entry<String, String> e : attr.entrySet()) {
                String k = ProcessDefinition.ATTRIBUTE_KEY_PREFIX + e.getKey();
                vars = vars.setVariable(k, e.getValue());
            }
        }

        if (args != null) {
            for (Map.Entry<String, Object> e : args.entrySet()) {
                vars = vars.setVariable(e.getKey(), e.getValue());
            }
        }

        vars = vars.setVariable(ExecutionContext.PROCESS_BUSINESS_KEY, state.getBusinessKey());

        return state.setVariables(vars);
    }

    public static ProcessInstance push(ProcessInstance state, ProcessElementCommand cmd) {
        CommandStack stack = state.getStack();
        return state.setStack(stack.push(cmd));
    }

    public static ProcessInstance push(ProcessInstance state, Action... actions) {
        CommandStack stack = state.getStack();
        return state.setStack(stack.push(new PerformActionsCommand(Arrays.asList(actions))));
    }

    @CoverageIgnore
    public static void dump(ProcessInstance state) {
        StringBuilder b = new StringBuilder();

        Collection<Command> commands = state.getStack().values();
        printCollection(b, commands);

        printScopes(b, state.getScopes());

        Map<UUID, Map<UUID, EventRecord>> events = state.getEvents().values();
        printEvents(b, events);

        if (PRINT_ACTIVATIONS) {
            Map<ActivationKey, Integer> activations = state.getActivations().values();
            printActivations(b, activations);
        }

        b.append("\n");

        log.trace("{}", b.toString());
    }

    @CoverageIgnore
    private static void printCollection(StringBuilder b, Collection<?> items) {
        b.append("\n=================================\n")
                .append("\t").append("COMMANDS").append(": ")
                .append(items.size())
                .append("\n");

        for (Iterator<?> i = items.iterator(); i.hasNext(); ) {
            Object o = i.next();
            b.append("\t\t").append(o);
            if (i.hasNext()) {
                b.append("\n");
            }
        }
    }

    @CoverageIgnore
    private static void printScopes(StringBuilder b, Scopes scopes) {
        Map<UUID, Scope> items = scopes.values();

        b.append("\n=================================\n")
                .append("\t").append("SCOPES").append(": ")
                .append(items.size())
                .append("\n")
                .append("\tCURRENT SCOPE: ").append(scopes.getCurrentId()).append("\n");

        for (Map.Entry<?, Scope> e : items.entrySet()) {
            Scope s = e.getValue();
            if (s.getParentId() != null) {
                continue;
            }

            printScopes(b, items, s.getId(), 2);
        }

        b.append("\tCURRENT SCOPE STACK:\n");

        UUID currentId = scopes.getCurrentId();
        if (currentId != null) {
            List<Scope> stack = scopes.traverse(currentId);
            for (Scope s : stack) {
                b.append("\t\t").append(s.getId()).append("=").append(s).append("\n");
            }
        }
    }

    @CoverageIgnore
    private static void printScopes(StringBuilder b, Map<UUID, Scope> scopes, UUID rootId, int level) {
        for (int i = 0; i < level; i++) {
            b.append("\t");
        }

        Scope s = scopes.get(rootId);
        b.append(rootId).append("=").append(s).append("\n");

        for (Map.Entry<?, Scope> e : scopes.entrySet()) {
            Scope next = e.getValue();
            if (rootId.equals(next.getParentId())) {
                printScopes(b, scopes, next.getId(), level + 1);
            }
        }
    }

    @CoverageIgnore
    private static void printEvents(StringBuilder b, Map<UUID, Map<UUID, EventRecord>> items) {
        b.append("\n=================================\n")
                .append("\t").append("EVENTS").append(": ")
                .append(items.size())
                .append("\n");

        for (Map.Entry<UUID, Map<UUID, EventRecord>> e : items.entrySet()) {
            Object k = e.getKey();
            Map<?, ?> v = e.getValue();
            if (v.isEmpty()) {
                b.append("\t\t").append(k).append(" = EMPTY\n");
                continue;
            }

            b.append("\t\t").append(k).append(" = {\n");

            for (Map.Entry<?, ?> ee : v.entrySet()) {
                Object kk = ee.getKey();
                Object vv = ee.getValue();
                b.append("\t\t\t").append(kk).append("=").append(vv).append("\n");
            }
            b.append("\t\t}\n");
        }
    }

    @CoverageIgnore
    private static void printActivations(StringBuilder b, Map<ActivationKey, Integer> items) {
        b.append("\n=================================\n")
                .append("\t").append("ACTIVATIONS").append(": ")
                .append(items.size())
                .append("\n");

        for (Map.Entry<ActivationKey, Integer> e : items.entrySet()) {
            ActivationKey k = e.getKey();
            Integer v = e.getValue();

            b.append("\t\t").append(k.getScopeId())
                    .append(" / ").append(k.getElementId())
                    .append(" = ").append(v)
                    .append("\n");
        }
    }

    private StateHelper() {
    }
}
