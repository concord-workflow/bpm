package io.takari.bpm;

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

public class IndexedProcessDefinition extends ProcessDefinition {

    private static final long serialVersionUID = 1L;

    // the indexes are not serialized, they are rebuilt when the definition is deserialized
    private transient Map<String, List<SequenceFlow>> outgoingFlows;
    private transient Map<String, List<BoundaryEvent>> boundaryEvents;
    private transient volatile String fingerprint;

    public IndexedProcessDefinition(ProcessDefinition pd) {
        super(pd.getId(), pd.getChildren(), pd.getAttributes() != null ? pd.getAttributes() : Collections.emptyMap());
        setName(pd.getName());
        this.outgoingFlows = indexOutgoingFlows();
        this.boundaryEvents = indexBoundaryEvents();
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

    public List<SequenceFlow> findOptionalOutgoingFlows(String from) {
        return outgoingFlows.get(from);
    }

    public List<BoundaryEvent> findOptionalBoundaryEvents(String attachedToRef) {
        return boundaryEvents.get(attachedToRef);
    }

    private Map<String, List<SequenceFlow>> indexOutgoingFlows() {
        Map<String, List<SequenceFlow>> m = new HashMap<>();
        indexOutgoingFlows0(this, m);
        return Map.copyOf(m);
    }

    private static void indexOutgoingFlows0(ProcessDefinition pd, Map<String, List<SequenceFlow>> accumulator) {
        for (AbstractElement e : pd.getChildren()) {
            String id = e.getId();
            List<SequenceFlow> l = findOutgoingFlows(pd, id);
            accumulator.put(id, List.copyOf(l));

            if (e instanceof ProcessDefinition) {
                indexOutgoingFlows0((ProcessDefinition) e, accumulator);
            }
        }
    }

    private static List<SequenceFlow> findOutgoingFlows(ProcessDefinition pd, String from) {
        List<SequenceFlow> result = new ArrayList<>();

        for (AbstractElement e : pd.getChildren()) {
            if (e instanceof SequenceFlow) {
                SequenceFlow f = (SequenceFlow) e;
                if (from.equals(f.getFrom())) {
                    result.add(f);
                }
            }
        }

        return result;
    }

    private Map<String, List<BoundaryEvent>> indexBoundaryEvents() {
        Map<String, List<BoundaryEvent>> m = new HashMap<>();
        indexBoundaryEvents0(this, m);
        return m;
    }

    private static void indexBoundaryEvents0(ProcessDefinition pd, Map<String, List<BoundaryEvent>> accumulator) {
        for (AbstractElement e : pd.getChildren()) {
            if (e instanceof ProcessDefinition) {
                indexBoundaryEvents0((ProcessDefinition) e, accumulator);
            }

            if (!(e instanceof BoundaryEvent)) {
                continue;
            }

            BoundaryEvent ev = (BoundaryEvent) e;
            if (ev.getAttachedToRef() == null) {
                continue;
            }

            List<BoundaryEvent> l = accumulator.computeIfAbsent(ev.getAttachedToRef(), k -> new ArrayList<>());
            l.add(ev);
        }
    }
}
