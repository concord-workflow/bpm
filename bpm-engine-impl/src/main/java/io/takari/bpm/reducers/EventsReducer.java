package io.takari.bpm.reducers;

import java.util.Date;
import java.util.UUID;

import io.takari.bpm.actions.*;
import io.takari.bpm.commands.ClearCommandStackCommand;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.ProcessDefinitionUtils;
import io.takari.bpm.UuidGenerator;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.el.ExpressionManager;
import io.takari.bpm.event.Event;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.model.IntermediateCatchEvent;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.state.EventMapHelper;
import io.takari.bpm.state.ProcessInstance;

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

        Event ev = makeEvent(state, a.getDefinitionId(), a.getElementId(), a.getGroupId(), a.isExclusive());

        IndexedProcessDefinition pd = state.getDefinition(a.getDefinitionId());
        SequenceFlow next = ProcessDefinitionUtils.findOutgoingFlow(pd, a.getElementId());

        if (a.getGroupId() != null) {
            // grouped event
            state = EventMapHelper.put(state, ev,
                    new ClearCommandStackCommand(pd.getId()),
                    new PerformActionsCommand(new PersistExecutionAction()),
                    new ProcessElementCommand(pd.getId(), next.getId(), a.getGroupId(), a.isExclusive()));
        } else {
            // standalone event
            state = state.setStack(state.getStack()
                    .push(new ProcessElementCommand(pd.getId(), next.getId(), a.getGroupId(), a.isExclusive()))
                    .push(new PerformActionsCommand(new SuspendAndPersistAction())));
        }

        eventManager.add(ev);

        return state;
    }

    private Event makeEvent(ProcessInstance state, String definitionId, String elementId, UUID groupId, boolean exclusive)
            throws ExecutionException {
        IndexedProcessDefinition pd = state.getDefinition(definitionId);
        IntermediateCatchEvent ice = (IntermediateCatchEvent) ProcessDefinitionUtils.findElement(pd, elementId);

        UUID id = uuidGenerator.generate();
        String name = getEventName(ice);

        Date timeDate = parseTimeDate(state, expressionManager, definitionId, elementId, ice.getTimeDate());
        Date timeDuration = parseExpiredAt(state, expressionManager, definitionId, elementId, ice.getTimeDuration());
        Date expiredAt = timeDate != null ? timeDate : timeDuration;

        return new Event(id, state.getId(), pd.getId(), groupId, name, state.getBusinessKey(), exclusive, expiredAt);
    }

    private static String getEventName(IntermediateCatchEvent e) {
        return e.getMessageRef() != null ? e.getMessageRef() : e.getId();
    }

    private static Date parseTimeDate(ProcessInstance state, ExpressionManager em, String definitionId, String elementId, String s)
            throws ExecutionException {
        ExecutionContextImpl ctx = new ExecutionContextImpl(em, state.getVariables());
        Object v = eval(s, ctx, em, Object.class);

        // expression evaluation may have side-effects, but they are ignored
        // there
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

    private static Date parseExpiredAt(ProcessInstance state, ExpressionManager em, String definitionId, String elementId, String s)
            throws ExecutionException {
        ExecutionContextImpl ctx = new ExecutionContextImpl(em, state.getVariables());
        Object v = eval(s, ctx, em, Object.class);

        // expression evaluation may have side-effects, but they are ignored
        // there
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
