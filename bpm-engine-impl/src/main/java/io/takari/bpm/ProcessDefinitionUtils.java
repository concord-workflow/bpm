package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.ProcessElementCommand;
import io.takari.bpm.misc.CoverageIgnore;
import io.takari.bpm.model.*;
import io.takari.bpm.state.Activations;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.Scopes;
import io.takari.bpm.utils.Timeout;
import org.joda.time.Duration;

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
        ProcessDefinition sub = findElementProcess0(pd, id);
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

    private static void fillQueue(Queue<FlowSignal> q, IndexedProcessDefinition pd, String elemId, int count) throws ExecutionException {
        findOptionalOutgoingFlows(pd, elemId).stream() //
                .map(s -> new FlowSignal(s, count)) //
                .forEach(q::add);

        for (BoundaryEvent be : findOptionalBoundaryEvents(pd, elemId)) {
            findOptionalOutgoingFlows(pd, be.getId()).stream() //
                    .map(s -> new FlowSignal(s, 0)) // assuming no errors at first
                    .forEach(q::add);
        }
    }

    private static class FlowSignal {
        final SequenceFlow flow;
        final int count;

        public FlowSignal(SequenceFlow flow, int count) {
            this.flow = flow;
            this.count = count;
        }
    }

    /**
     * Collects all (not just the first ones) downstream parallel gateways' incoming flows
     */
    private static List<FlowSignal> findDownstreamGatewayFlows(IndexedProcessDefinition pd, String from, int count) throws ExecutionException {
        Set<String> memento = new HashSet<>();
        Queue<FlowSignal> processingQueue = new LinkedList<>();

        List<FlowSignal> results = new ArrayList<>();

        AbstractElement elem = findElement(pd, from);
        if (elem instanceof SequenceFlow) {
            processingQueue.add(new FlowSignal((SequenceFlow) elem, count));
        } else {
            fillQueue(processingQueue, pd, elem.getId(), count);
        }

        while (!processingQueue.isEmpty()) {
            FlowSignal fs = processingQueue.poll();

            if (!memento.add(fs.flow.getId())) {
                continue;
            }

            AbstractElement target = findElement(pd, fs.flow.getTo());
            if (target instanceof EndEvent) {
                continue;
            }

            if (isParallelGateway(target)) {
                results.add(fs);
                continue;
            }

            fillQueue(processingQueue, pd, target.getId(), fs.count);
        }

        return results;
    }

    public static boolean isParallelGateway(AbstractElement e) {
        return e instanceof ParallelGateway;
    }

    public static ProcessInstance activateGatewayFlow(ProcessInstance state, IndexedProcessDefinition pd, String elementId, int count) throws ExecutionException {
        Activations acts = state.getActivations();
        Scopes scopes = state.getScopes();
        UUID scopeId = scopes.getCurrentId();
        for (FlowSignal fs : findDownstreamGatewayFlows(pd, elementId, count)) {
            acts = acts.incExpectation(scopes, scopeId, fs.flow.getId(), fs.count);
        }
        return state.setActivations(acts);
    }

    /**
     * Tests whether the normal (non-error) flow can be traced to the specified element
     */
    public static boolean isTracedToElement(IndexedProcessDefinition pd, String flowId, String elementId) throws ExecutionException {
        Set<String> memento = new HashSet<>();
        Queue<String> processingQueue = new LinkedList<>();
        processingQueue.add(flowId);
        while (!processingQueue.isEmpty()) {
            String nextId = processingQueue.poll();

            if (nextId.equals(elementId)) {
                return true;
            }

            if (!memento.add(nextId)) {
                continue;
            }
            AbstractElement target = findElement(pd, nextId);
            if (target instanceof SequenceFlow) {
                processingQueue.add(((SequenceFlow) target).getTo());
                continue;
            }
            findOptionalOutgoingFlows(pd, nextId).stream() //
                    .forEach(f -> processingQueue.add(f.getId()));
        }

        return false;
    }

    public static List<String> toIds(List<? extends AbstractElement> elements) {
        List<String> result = new ArrayList<>(elements.size());
        for (AbstractElement e : elements) {
            result.add(e.getId());
        }
        return result;
    }

    private static EventBasedGateway findEventGateway(ProcessDefinition pd, String eventElementId) throws ExecutionException {
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
                l.add(new Timeout<>(d.getMillis(), c));
            }
        }

        l.sort((o1, o2) -> (int) (o1.getDuration() - o2.getDuration()));

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

    @CoverageIgnore
    private ProcessDefinitionUtils() {
    }
}
