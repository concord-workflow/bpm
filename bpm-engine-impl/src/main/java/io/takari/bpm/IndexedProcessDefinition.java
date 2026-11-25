package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.AbstractElement;
import io.takari.bpm.model.BoundaryEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IndexedProcessDefinition extends ProcessDefinition {

    private static final long serialVersionUID = 1L;

    // the indexes are not serialized, they are rebuilt when the definition is deserialized
    private transient Map<String, AbstractElement> elements;
    private transient Map<String, ProcessDefinition> elementProcesses;
    private transient Map<String, List<SequenceFlow>> outgoingFlows;
    private transient Map<String, List<SequenceFlow>> incomingFlows;
    private transient Map<String, List<BoundaryEvent>> boundaryEvents;
    private transient Map<String, Map<String, Integer>> reachableJoinFlows;
    private transient volatile String fingerprint;

    public IndexedProcessDefinition(ProcessDefinition pd) {
        super(pd.getId(), pd.getChildren(), pd.getAttributes() != null ? pd.getAttributes() : Collections.emptyMap());
        setName(pd.getName());

        Map<String, AbstractElement> elements = new HashMap<>();
        Map<String, ProcessDefinition> elementProcesses = new HashMap<>();
        Map<String, List<SequenceFlow>> outgoingFlows = new HashMap<>();
        Map<String, List<SequenceFlow>> incomingFlows = new HashMap<>();
        Map<String, List<BoundaryEvent>> boundaryEvents = new HashMap<>();
        index(this, elements, elementProcesses, outgoingFlows, incomingFlows, boundaryEvents);

        this.elements = elements;
        this.elementProcesses = elementProcesses;
        this.outgoingFlows = freeze(outgoingFlows);
        this.incomingFlows = freeze(incomingFlows);
        this.boundaryEvents = freeze(boundaryEvents);
        this.reachableJoinFlows = new ConcurrentHashMap<>();
    }

    private Object readResolve() {
        return new IndexedProcessDefinition(this);
    }

    /**
     * @return a hash of the definition's content.
     */
    public String getFingerprint() {
        String result = fingerprint;
        if (result == null) {
            result = fingerprint = computeFingerprint(this);
        }
        return result;
    }

    private static String computeFingerprint(ProcessDefinition pd) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (ObjectOutputStream out = new ObjectOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), md))) {
                out.writeObject(new ArrayList<>(pd.getChildren()));
                out.writeObject(pd.getAttributes() != null ? new TreeMap<>(pd.getAttributes()) : null);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException("Can't compute the fingerprint of the process definition: " + pd.getId(), e);
        }
    }

    /**
     * @return the outgoing flows of the element, an empty list if the element has no outgoing flows
     * or {@code null} if there's no such element.
     */
    public List<SequenceFlow> findOptionalOutgoingFlows(String from) {
        return outgoingFlows.get(from);
    }

    /**
     * @return the incoming flows of the element or an empty list.
     */
    public List<SequenceFlow> findIncomingFlows(String to) {
        return incomingFlows.getOrDefault(to, List.of());
    }

    public List<BoundaryEvent> findOptionalBoundaryEvents(String attachedToRef) {
        return boundaryEvents.get(attachedToRef);
    }

    /**
     * @return the element with the specified ID (including the elements of subprocesses) or {@code null}.
     */
    public AbstractElement findOptionalElement(String id) {
        return elements.get(id);
    }

    /**
     * @return the (sub)process that contains the element or {@code null}.
     */
    public ProcessDefinition findOptionalElementProcess(String id) {
        return elementProcesses.get(id);
    }

    /**
     * Returns the cached result of the function. The definition is immutable, so are the results.
     */
    Map<String, Integer> getReachableJoinFlows(String id, ReachFunction fn) throws ExecutionException {
        Map<String, Integer> result = reachableJoinFlows.get(id);
        if (result == null) {
            result = Collections.unmodifiableMap(fn.compute(id));
            reachableJoinFlows.putIfAbsent(id, result);
        }
        return result;
    }

    interface ReachFunction {

        Map<String, Integer> compute(String id) throws ExecutionException;
    }

    private static void index(ProcessDefinition pd,
                              Map<String, AbstractElement> elements,
                              Map<String, ProcessDefinition> elementProcesses,
                              Map<String, List<SequenceFlow>> outgoingFlows,
                              Map<String, List<SequenceFlow>> incomingFlows,
                              Map<String, List<BoundaryEvent>> boundaryEvents) {

        for (AbstractElement e : pd.getChildren()) {
            elements.put(e.getId(), e);
            elementProcesses.put(e.getId(), pd);
            outgoingFlows.putIfAbsent(e.getId(), new ArrayList<>());
        }

        for (AbstractElement e : pd.getChildren()) {
            if (e instanceof SequenceFlow f) {
                outgoingFlows.computeIfAbsent(f.getFrom(), k -> new ArrayList<>()).add(f);
                incomingFlows.computeIfAbsent(f.getTo(), k -> new ArrayList<>()).add(f);
            }

            if (e instanceof BoundaryEvent ev && ev.getAttachedToRef() != null) {
                boundaryEvents.computeIfAbsent(ev.getAttachedToRef(), k -> new ArrayList<>()).add(ev);
            }

            if (e instanceof ProcessDefinition sub) {
                index(sub, elements, elementProcesses, outgoingFlows, incomingFlows, boundaryEvents);
            }
        }
    }

    private static <T> Map<String, List<T>> freeze(Map<String, List<T>> m) {
        Map<String, List<T>> result = new HashMap<>(m.size());
        for (Map.Entry<String, List<T>> e : m.entrySet()) {
            result.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}
