package io.takari.bpm.reducers;

import com.google.common.collect.Lists;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.UuidGenerator;
import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.CreateEventAction;
import io.takari.bpm.actions.PopScopeAction;
import io.takari.bpm.actions.SetCurrentScopeAction;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.Events;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes.Scope;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Impure
public class EventsReducer implements Reducer {

    private static final Logger log = LoggerFactory.getLogger(EventsReducer.class);

    private final UuidGenerator uuidGenerator;
    private final ExpressionManager expressionManager;
    private final EventPersistenceManager eventManager;

    public EventsReducer(UuidGenerator uuidGenerator, ExpressionManager expressionManager, EventPersistenceManager eventManager) {
        this.uuidGenerator = uuidGenerator;
        this.expressionManager = expressionManager;
        this.eventManager = eventManager;
    }

    @Override
    public ProcessInstance reduce(ProcessInstance state, Action action) throws ExecutionException {
        if (!(action instanceof CreateEventAction)) {
            return state;
        }

        CreateEventAction a = (CreateEventAction) action;

        IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
        SequenceFlow next = ProcessDefinitionUtils.findOutgoingFlow(pd, a.getElementId());

        // TODO move to an utility fn
        Scope scope = state.getScopes().peek();
        List<Scope> scopes = state.getScopes().traverse(scope.getId());

        // create a list of commands that will be executed on activation of an event
        List<Command> cmds = new ArrayList<>();

        addScopeClosingActions(cmds, scopes);

        // evaluate the following element after the event
        cmds.add(new ProcessElementCommand(pd.getId(), next.getId()));

        // restore the scope on resume
        cmds.add(new PerformActionsCommand(new SetCurrentScopeAction(scope.getId())));

        // create and save an event
        Events events = state.getEvents();
        Event ev = makeEvent(state, a);
        state = state.setEvents(events.addEvent(ev.getScopeId(), ev.getId(), ev.getName(), cmds.toArray(new Command[cmds.size()])));
        eventManager.add(ev);

        return state;
    }

    private Event makeEvent(ProcessInstance state, CreateEventAction action)
            throws ExecutionException {

        ExecutionContextImpl ctx = new ExecutionContextImpl(expressionManager, state.getVariables());

        UUID id = uuidGenerator.generate();
        String name = getEventName(action, ctx, expressionManager);

        Date timeDate = parseTimeDate(state, ctx, expressionManager, action);
        Date timeDuration = parseExpiredAt(state, expressionManager, action);
        Date expiredAt = timeDate != null ? timeDate : timeDuration;

        Scope s = state.getScopes().peek();
        UUID scopeId = s.getId();
        boolean exclusive = s.isExclusive();

        return new Event(id, state.getId(), action.getDefinitionId(), scopeId, name, state.getBusinessKey(), exclusive, expiredAt);
    }

    private static void addScopeClosingActions(Collection<Command> cmds, List<Scope> currentStack) {

        for (Scope s : Lists.reverse(currentStack)) {
            // scope finishers must pop their scopes themselves
            if (s.getFinishers() == null) {
                cmds.add(new PerformActionsCommand(new PopScopeAction()));
            } else {
                Collections.addAll(cmds, s.getFinishers());
            }
        }
    }

    private static String getEventName(CreateEventAction a, ExecutionContext ctx, ExpressionManager em) {
        String msgRefExpr = a.getMessageRefExpression();

        if (msgRefExpr != null) {
            return em.eval(ctx, msgRefExpr, String.class);
        }

        String msgRef = a.getMessageRef();
        return msgRef != null ? msgRef : a.getElementId();
    }

    private static Date parseTimeDate(ProcessInstance state, ExecutionContextImpl ctx, ExpressionManager em, CreateEventAction a)
            throws ExecutionException {

        String definitionId = a.getDefinitionId();
        String elementId = a.getElementId();
        String s = a.getTimeDate();

        Object v = eval(s, ctx, em, Object.class);

        // expression evaluation may have side-effects, but they are ignored
        if (!ctx.toActions().isEmpty()) {
            log.warn("parseTimeData ['{}', '{}', '{}', '{}'] -> variables changes in the execution context will be ignored",
                    state.getBusinessKey(), definitionId, elementId, s);
        }

        if (v == null) {
            return null;
        }

        if (v instanceof String) {
            return parseIso8601(s);
        } else if (v instanceof Date) {
            return (Date) v;
        } else {
            throw new ExecutionException("Invalid timeDate format in the process '%s' in the element '%s': '%s'",
                    definitionId, elementId, s);
        }
    }

    private static Date parseExpiredAt(ProcessInstance state, ExpressionManager em, CreateEventAction a)
            throws ExecutionException {

        String definitionId = a.getDefinitionId();
        String elementId = a.getElementId();
        String s = a.getTimeDuration();

        ExecutionContextImpl ctx = new ExecutionContextImpl(em, state.getVariables());
        Object v = eval(s, ctx, em, Object.class);

        // expression evaluation may have side-effects, but they are ignored
        if (!ctx.toActions().isEmpty()) {
            log.warn("parseExpiredAt ['{}', '{}', '{}', '{}'] -> variables changes in the execution context will be ignored",
                    state.getBusinessKey(), definitionId, elementId, s);
        }

        if (v == null) {
            return null;
        }

        if (v instanceof String && isDuration(v.toString())) {
            return DateTime.now().plus(Period.parse(v.toString())).toDate();
        } else {
            throw new ExecutionException("Invalid duration format in the process '%s' in the element '%s': '%s'",
                    definitionId, elementId, s);
        }
    }

    private static <T> T eval(String expr, ExecutionContext ctx, ExpressionManager em, Class<T> type) {
        if (expr == null || expr.trim().isEmpty()) {
            return null;
        }
        return em.eval(ctx, expr, type);
    }

    public static Date parseIso8601(String s) {
        return DateTime.parse(s).toDate();
    }

    private static boolean isDuration(String time) {
        return time.startsWith("P");
    }
}
