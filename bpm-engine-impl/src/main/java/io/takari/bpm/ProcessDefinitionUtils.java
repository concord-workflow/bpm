package io.takari.bpm;

import io.takari.bpm.actions.Action;
import io.takari.bpm.actions.InclusiveForkAction;
import io.takari.bpm.actions.ParallelForkAction;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.PerformActionsCommand;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.model.*;
import io.takari.bpm.state.Activations;
import io.takari.bpm.state.Activations.Activation;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;
import io.takari.bpm.utils.Timeout;

import java.time.Duration;
import java.util.*;

public final class ProcessDefinitionUtils {

    /**
     * Finds a (sub)process definition by its element ID.
     *
     * @param pd a parent process definition.
     * @param id a (sub)process element ID.
     * @return the process definition, which contains an element with the
     * specified ID.
     * @throws ExecutionException if the element is not found in the parent
     *                            process or any of its subprocesses.
     */
    public static ProcessDefinition findElementProcess(ProcessDefinition pd, String id) throws ExecutionException {
        ProcessDefinition sub = pd instanceof IndexedProcessDefinition ipd
                ? ipd.findOptionalElementProcess(id)
                : findElementProcess0(pd, id);
        if (sub == null) {
            throw new ExecutionException("Invalid process definition '%s': unknown element '%s'", pd.getId(), id);
        }
        return sub;
    }

    private static ProcessDefinition findElementProcess0(ProcessDefinition pd, String id) {
        if (pd.hasChild(id)) {
            return pd;
        }

        for (AbstractElement e : pd.getChildren()) {
            if (e instanceof ProcessDefinition) {
                ProcessDefinition sub = findElementProcess0((ProcessDefinition) e, id);
                if (sub != null) {
                    return sub;
                }
            }
        }

        return null;
    }

    /**
     * Finds an element of (sub)process by its ID;
     *
     * @param pd
     * @param id
     * @throws ExecutionException if the element is not found.
     */
    public static AbstractElement findElement(ProcessDefinition pd, String id) throws ExecutionException {
        if (pd instanceof IndexedProcessDefinition ipd) {
            AbstractElement e = ipd.findOptionalElement(id);
            if (e == null) {
                throw new ExecutionException("Invalid process definition '%s': unknown element '%s'", pd.getId(), id);
            }
            return e;
        }

        ProcessDefinition sub = findElementProcess(pd, id);
        return sub.getChild(id);
    }

    /**
     * Finds a subprocess by its ID.
     *
     * @param pd
     * @param id
     * @throws ExecutionException if the subprocess is not found.
     */
    public static SubProcess findSubProcess(ProcessDefinition pd, String id) throws ExecutionException {
        AbstractElement e = findElement(pd, id);
        if (e instanceof SubProcess) {
            return (SubProcess) e;
        } else {
            throw new ExecutionException("Invalid process definition '%s': element '%s' is not a subprocess element", pd.getId(), id);
        }
    }

    public static List<SequenceFlow> findOptionalOutgoingFlows(IndexedProcessDefinition pd, String from) throws ExecutionException {
        return pd.findOptionalOutgoingFlows(from);
    }

    /**
     * Finds all outgoing flows for the specified element.
     *
     * @param pd
     * @param from
     * @throws ExecutionException if the element has no outgoing flows..
     */
    public static List<SequenceFlow> findOutgoingFlows(IndexedProcessDefinition pd, String from) throws ExecutionException {
        List<SequenceFlow> result = findOptionalOutgoingFlows(pd, from);

        if (result.isEmpty()) {
            throw new ExecutionException("Invalid process definition '%s': no flows from '%s'", pd.getId(), from);
        }

        return result;
    }

    public static SequenceFlow findOutgoingFlow(IndexedProcessDefinition pd, String from) throws ExecutionException {
        List<SequenceFlow> l = findOutgoingFlows(pd, from);
        if (l.size() != 1) {
            throw new ExecutionException("Invalid process definition '%s': expected single flow from '%s'", pd.getId(), from);
        }
        return l.get(0);
    }

    public static SequenceFlow findAnyOutgoingFlow(IndexedProcessDefinition pd, String from) throws ExecutionException {
        List<SequenceFlow> l = findOutgoingFlows(pd, from);
        return l.get(0);
    }

    /**
     * Finds all incoming flows for the specified element.
     *
     * @param pd
     * @param to
     * @throws ExecutionException if the element has no incoming flows..
     */
    public static List<SequenceFlow> findIncomingFlows(ProcessDefinition pd, String to) throws ExecutionException {
        if (pd instanceof IndexedProcessDefinition ipd) {
            List<SequenceFlow> result = ipd.findIncomingFlows(to);
            if (result.isEmpty()) {
                throw new ExecutionException("Invalid process definition '%s': no flows to '%s'", pd.getId(), to);
            }
            return result;
        }

        List<SequenceFlow> result = new ArrayList<>();

        ProcessDefinition sub = findElementProcess(pd, to);
        for (AbstractElement e : sub.getChildren()) {
            if (e instanceof SequenceFlow) {
                SequenceFlow f = (SequenceFlow) e;
                if (to.equals(f.getTo())) {
                    result.add(f);
                }
            }
        }

        if (result.isEmpty()) {
            throw new ExecutionException("Invalid process definition '%s': no flows to '%s'", pd.getId(), to);
        }

        return result;
    }

    /**
     * Finds a (first) start event of the process.
     *
     * @param pd
     * @throws ExecutionException if process has no start events.
     */
    public static StartEvent findStartEvent(ProcessDefinition pd) throws ExecutionException {
        for (AbstractElement e : pd.getChildren()) {
            if (e instanceof StartEvent) {
                return (StartEvent) e;
            }
        }

        throw new ExecutionException("Invalid process definition '%s': no start event defined", pd.getId());
    }

    public static List<BoundaryEvent> findOptionalBoundaryEvents(IndexedProcessDefinition pd, String attachedToRef) throws ExecutionException {
        List<BoundaryEvent> l = pd.findOptionalBoundaryEvents(attachedToRef);
        return l != null ? l : Collections.emptyList();
    }

    public static BoundaryEvent findBoundaryErrorEvent(IndexedProcessDefinition pd, String attachedToRef, String errorRef) throws ExecutionException {
        List<BoundaryEvent> l = findOptionalBoundaryEvents(pd, attachedToRef);
        for (BoundaryEvent ev : l) {
            if (attachedToRef.equals(ev.getAttachedToRef())) {
                if (errorRef != null) {
                    if (errorRef.equals(ev.getErrorRef())) {
                        return ev;
                    }
                } else if (ev.getErrorRef() == null && ev.getTimeDuration() == null) {
                    return ev;
                }
            }
        }
        return null;
    }

    /**
     * Computes the incoming flows of the parallel and inclusive gateways that a single token, positioned at
     * the specified element (or sequence flow), can reach next. The search stops at the first gateway on each path.
     * <p>
     * The result maps the gateway's incoming flow ID to the number of tokens expected to arrive on that flow:
     * <ul>
     *     <li>exclusive and event-based gateways send the token to only one of their outgoing flows, so the
     *     alternatives are merged using {@code max} (a token arrives at most once, regardless of the path taken);</li>
     *     <li>other elements send a token into each of their outgoing flows, so the counts are added;</li>
     *     <li>flows reachable only through boundary events are included with {@code 0}: no tokens are expected,
     *     but the flow is known to be "activated".</li>
     * </ul>
     * Loops are cut when the search returns to an element that's already on the current path.
     */
    public static Map<String, Integer> findReachableJoinFlows(IndexedProcessDefinition pd, String fromId) throws ExecutionException {
        // the result depends only on the definition, it is computed once
        return pd.getReachableJoinFlows(fromId, id -> reach(pd, id, new HashSet<>(), new HashMap<>()).flows);
    }

    private static final class Reach {

        private static final Reach CUT = new Reach(Collections.emptyMap(), true);

        private final Map<String, Integer> flows;
        private final boolean cut;

        private Reach(Map<String, Integer> flows, boolean cut) {
            this.flows = flows;
            this.cut = cut;
        }
    }

    private static Reach reach(IndexedProcessDefinition pd, String id, Set<String> path, Map<String, Map<String, Integer>> memo) throws ExecutionException {
        Map<String, Integer> cached = memo.get(id);
        if (cached != null) {
            return new Reach(cached, false);
        }

        if (!path.add(id)) {
            return Reach.CUT;
        }

        try {
            Map<String, Integer> result = new HashMap<>();
            boolean cut = false;

            AbstractElement e = findElement(pd, id);
            if (e instanceof SequenceFlow f) {
                AbstractElement target = findElement(pd, f.getTo());
                if (isParallelGateway(target)) {
                    result.put(f.getId(), 1);
                } else {
                    Reach r = reach(pd, target.getId(), path, memo);
                    result.putAll(r.flows);
                    cut = r.cut;
                }
            } else if (!(e instanceof EndEvent)) {
                boolean exclusive = e instanceof ExclusiveGateway || e instanceof EventBasedGateway;

                List<SequenceFlow> out = findOptionalOutgoingFlows(pd, id);
                if (out != null) {
                    for (SequenceFlow f : out) {
                        Reach r = reach(pd, f.getId(), path, memo);
                        for (Map.Entry<String, Integer> x : r.flows.entrySet()) {
                            if (exclusive) {
                                result.merge(x.getKey(), x.getValue(), Math::max);
                            } else {
                                result.merge(x.getKey(), x.getValue(), Integer::sum);
                            }
                        }
                        cut |= r.cut;
                    }
                }

                for (BoundaryEvent be : findOptionalBoundaryEvents(pd, id)) {
                    // assuming no errors at first
                    Reach r = reach(pd, be.getId(), path, memo);
                    for (String k : r.flows.keySet()) {
                        result.putIfAbsent(k, 0);
                    }
                    cut |= r.cut;
                }
            }

            if (!cut) {
                memo.put(id, result);
            }

            return new Reach(result, cut);
        } finally {
            path.remove(id);
        }
    }

    public static boolean isParallelGateway(AbstractElement e) {
        return e instanceof ParallelGateway;
    }

    /**
     * Registers the expected join activations for {@code count} tokens positioned at the specified element.
     * A negative {@code count} removes the expectations.
     */
    public static ProcessInstance activateGatewayFlow(ProcessInstance state, IndexedProcessDefinition pd, String elementId, int count) throws ExecutionException {
        Activations acts = state.getActivations();
        Scopes scopes = state.getScopes();
        UUID scopeId = scopes.getCurrentId();
        for (Map.Entry<String, Integer> e : findReachableJoinFlows(pd, elementId).entrySet()) {
            acts = acts.incExpectation(scopes, scopeId, e.getKey(), e.getValue() * count);
        }
        state = state.setActivations(acts);

        if (count < 0) {
            state = recheckJoins(state, pd, findReachableJoinFlows(pd, elementId).keySet());
        }

        return state;
    }

    /**
     * Moves a token from one position to another (e.g. when an exclusive gateway selects one of its flows or
     * when an activity is interrupted by a boundary event). The expected join activations are adjusted to the
     * difference between what's reachable from the new position and what was reachable from the old one.
     * Joins that no longer wait for the token are re-evaluated.
     */
    public static ProcessInstance retarget(ProcessInstance state, IndexedProcessDefinition pd, Collection<String> fromIds, Collection<String> toIds) throws ExecutionException {
        Map<String, Integer> before = new HashMap<>();
        for (String id : fromIds) {
            findReachableJoinFlows(pd, id).forEach((k, v) -> before.merge(k, v, Integer::sum));
        }

        Map<String, Integer> after = new HashMap<>();
        for (String id : toIds) {
            findReachableJoinFlows(pd, id).forEach((k, v) -> after.merge(k, v, Integer::sum));
        }

        Activations acts = state.getActivations();
        Scopes scopes = state.getScopes();
        UUID scopeId = scopes.getCurrentId();

        Set<String> decreased = new LinkedHashSet<>();
        Set<String> keys = new LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (String k : keys) {
            int diff = after.getOrDefault(k, 0) - before.getOrDefault(k, 0);
            if (diff == 0 && !after.containsKey(k)) {
                continue;
            }

            acts = acts.incExpectation(scopes, scopeId, k, diff);
            if (diff < 0) {
                decreased.add(k);
            }
        }

        state = state.setActivations(acts);
        return recheckJoins(state, pd, decreased);
    }

    /**
     * Re-evaluates the joins of the specified flows. Joins that are complete and have tokens waiting
     * are scheduled for activation.
     */
    private static ProcessInstance recheckJoins(ProcessInstance state, IndexedProcessDefinition pd, Collection<String> flowIds) throws ExecutionException {
        Set<String> gateways = new LinkedHashSet<>();
        for (String flowId : flowIds) {
            SequenceFlow f = (SequenceFlow) findElement(pd, flowId);
            gateways.add(f.getTo());
        }

        for (String gwId : gateways) {
            if (!isJoinComplete(state, pd, gwId, true)) {
                continue;
            }

            AbstractElement gw = findElement(pd, gwId);
            Action a = gw instanceof InclusiveGateway
                    ? new InclusiveForkAction(pd.getId(), gwId)
                    : new ParallelForkAction(pd.getId(), gwId);

            state = state.setStack(state.getStack().push(new PerformActionsCommand(a)));
        }

        return state;
    }

    /**
     * Checks whether all expected tokens arrived to the specified join.
     *
     * @param requireTokens if {@code true}, the join must have at least one token waiting.
     */
    public static boolean isJoinComplete(ProcessInstance state, IndexedProcessDefinition pd, String gatewayId, boolean requireTokens) throws ExecutionException {
        Activations acts = state.getActivations();
        Scopes scopes = state.getScopes();
        UUID scopeId = scopes.getCurrentId();

        boolean hasTokens = false;
        for (SequenceFlow flow : findIncomingFlows(pd, gatewayId)) {
            Activation act = acts.getActivation(scopes, scopeId, flow.getId());
            if (!act.isActivated() || act.getReceived() < act.getExpected()) {
                return false;
            }
            hasTokens |= act.getReceived() > 0;
        }

        return hasTokens || !requireTokens;
    }

    public static List<String> toIds(List<? extends AbstractElement> elements) {
        List<String> result = new ArrayList<>(elements.size());
        for (AbstractElement e : elements) {
            result.add(e.getId());
        }
        return result;
    }

    public static EventBasedGateway findEventGateway(ProcessDefinition pd, String eventElementId) throws ExecutionException {
        for (SequenceFlow flow : findIncomingFlows(pd, eventElementId)) {
            AbstractElement elem = findElement(pd, flow.getFrom());
            if (elem instanceof EventBasedGateway) {
                return (EventBasedGateway) elem;
            }
        }
        return null;
    }

    public static List<IntermediateCatchEvent> findSiblingEvents(IndexedProcessDefinition pd, String eventElementId) throws ExecutionException {
        EventBasedGateway gate = findEventGateway(pd, eventElementId);
        List<IntermediateCatchEvent> events = new ArrayList<>();
        if (gate != null) {
            for (SequenceFlow flow : findOutgoingFlows(pd, gate.getId())) {
                AbstractElement elem = findElement(pd, flow.getTo());
                if (elem instanceof IntermediateCatchEvent) {
                    events.add((IntermediateCatchEvent) elem);
                }
            }
        }
        return events;
    }

    public static List<SequenceFlow> findFlows(ProcessDefinition pd, List<String> ids) throws ExecutionException {
        List<SequenceFlow> result = new ArrayList<>();
        for (String id : ids) {
            SequenceFlow f = (SequenceFlow) findElement(pd, id);
            result.add(f);
        }
        return result;
    }

    public static List<Timeout<Command>> findTimers(IndexedProcessDefinition pd, ProcessElementCommand cmd) throws ExecutionException {
        List<BoundaryEvent> events = ProcessDefinitionUtils.findOptionalBoundaryEvents(pd, cmd.getElementId());
        List<Timeout<Command>> l = new ArrayList<>(events.size());
        for (BoundaryEvent ev : events) {
            if (ev.getTimeDuration() != null) {
                Duration d = Duration.parse(ev.getTimeDuration());
                Command c = new ProcessElementCommand(pd.getId(), ev.getId());
                l.add(new Timeout<>(d.toMillis(), c));
            }
        }

        l.sort(Comparator.comparingLong(Timeout::getDuration));

        return l;
    }

    public static Command findDefaultError(IndexedProcessDefinition pd, ProcessElementCommand cmd) throws ExecutionException {
        List<BoundaryEvent> events = ProcessDefinitionUtils.findOptionalBoundaryEvents(pd, cmd.getElementId());
        for (BoundaryEvent ev : events) {
            if (ev.getErrorRef() == null && ev.getTimeDuration() == null) {
                return new ProcessElementCommand(pd.getId(), ev.getId());
            }
        }

        return null;
    }

    public static Map<String, Command> findErrors(IndexedProcessDefinition pd, ProcessElementCommand cmd) throws ExecutionException {
        Map<String, Command> m = new HashMap<>();

        List<BoundaryEvent> events = ProcessDefinitionUtils.findOptionalBoundaryEvents(pd, cmd.getElementId());
        for (BoundaryEvent ev : events) {
            if (ev.getErrorRef() != null && ev.getTimeDuration() == null) {
                m.put(ev.getErrorRef(), new ProcessElementCommand(pd.getId(), ev.getId()));
            }
        }

        return m;
    }

    private ProcessDefinitionUtils() {
    }
}
