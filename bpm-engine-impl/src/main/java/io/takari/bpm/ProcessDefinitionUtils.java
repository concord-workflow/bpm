package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static String findNextGatewayId(IndexedProcessDefinition pd, String from) throws ExecutionException {
        AbstractElement e = findElement(pd, from);
        if (!(e instanceof SequenceFlow)) {
            e = findAnyOutgoingFlow(pd, from);
        }

        while (e != null) {
            if (e instanceof SequenceFlow) {
                SequenceFlow f = (SequenceFlow) e;
                e = findElement(pd, f.getTo());
            } else if (e instanceof ParallelGateway || e instanceof EventBasedGateway || e instanceof InclusiveGateway || e instanceof ExclusiveGateway) {
                return e.getId();
            } else if (e instanceof EndEvent) {
                return null;
            } else {
                e = findOutgoingFlow(pd, e.getId());
            }
        }

        throw new ExecutionException("Invalid process definition '%s': can't find next parallel gateway after '%s'", pd.getId(), from);
    }

    public static List<String> toIds(List<? extends AbstractElement> elements) {
        List<String> result = new ArrayList<>(elements.size());
        for (AbstractElement e : elements) {
            result.add(e.getId());
        }
        return result;
    }

    public static List<SequenceFlow> findFlows(ProcessDefinition pd, List<String> ids) throws ExecutionException {
        List<SequenceFlow> result = new ArrayList<>();
        for (String id : ids) {
            SequenceFlow f = (SequenceFlow) findElement(pd, id);
            result.add(f);
        }
        return result;
    }

    private ProcessDefinitionUtils() {
    }
}
