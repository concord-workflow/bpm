package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.state.ProcessInstance;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The size of the stored state doesn't grow with the number of executed steps.
 */
public class StateSizeTest extends AbstractEngineTest {

    private static final ThreadLocal<RecordingPersistenceManager> PM = new ThreadLocal<>();

    private final RecordingPersistenceManager pm;

    public StateSizeTest() {
        super(newPersistenceManager());
        this.pm = PM.get();
        PM.remove();
    }

    private static RecordingPersistenceManager newPersistenceManager() {
        RecordingPersistenceManager pm = new RecordingPersistenceManager();
        PM.set(pm);
        return pm;
    }

    /**
     * start --> inc --> gw1 --> t1 --> gw2 --> xor --> ev --> end
     *           ^          \          /          |
     *           |           --> t2 ---           |
     *           ------------- (loop) ------------
     */
    @Test
    public void testParallelLoop() throws Exception {
        getServiceTaskRegistry().register("inc", (JavaDelegate) ctx -> {
            Long l = (Long) ctx.getVariable("cnt");
            ctx.setVariable("cnt", l == null ? 1L : l + 1);
        });
        getServiceTaskRegistry().register("noop", (JavaDelegate) ctx -> {
        });

        Map<Integer, Integer> sizes = new HashMap<>();
        for (int loops : new int[]{10, 1000}) {
            String processId = "test" + loops;
            deploy(new ProcessDefinition(processId, Arrays.asList(
                    new StartEvent("start"),
                    new SequenceFlow("f1", "start", "inc"),
                    new ServiceTask("inc", ExpressionType.DELEGATE, "${inc}"),
                    new SequenceFlow("f2", "inc", "gw1"),
                    new ParallelGateway("gw1"),
                    new SequenceFlow("f3", "gw1", "t1"),
                    new ServiceTask("t1", ExpressionType.DELEGATE, "${noop}"),
                    new SequenceFlow("f4", "t1", "gw2"),
                    new SequenceFlow("f5", "gw1", "t2"),
                    new ServiceTask("t2", ExpressionType.DELEGATE, "${noop}"),
                    new SequenceFlow("f6", "t2", "gw2"),
                    new ParallelGateway("gw2"),
                    new SequenceFlow("f7", "gw2", "xor"),
                    new ExclusiveGateway("xor", "f8"),
                    new SequenceFlow("f8", "xor", "inc"),
                    new SequenceFlow("f9", "xor", "ev", "${cnt >= " + loops + "}"),
                    new IntermediateCatchEvent("ev", "ev"),
                    new SequenceFlow("f10", "ev", "end"),
                    new EndEvent("end"))));

            String key = UUID.randomUUID().toString();
            getEngine().start(key, processId, null);

            ProcessInstance s = pm.last;
            assertTrue(s.getScopes().values().size() <= 2, "scopes: " + s.getScopes().values().size());
            assertTrue(s.getActivations().values().size() <= 2, "activations: " + s.getActivations().values().size());
            sizes.put(loops, serialize(s).length);

            getEngine().resume(key, "ev", null);
        }

        // the only difference is the value of the counter and the process ID
        assertTrue(Math.abs(sizes.get(1000) - sizes.get(10)) < 64, "sizes: " + sizes);
    }

    @Test
    public void testIndexesAreRestoredAfterDeserialization() throws Exception {
        IndexedProcessDefinition pd = new IndexedProcessDefinition(bigDefinition("test", 10));

        IndexedProcessDefinition copy = (IndexedProcessDefinition) deserialize(serialize(pd));

        assertEquals("f1", copy.findOptionalOutgoingFlows("t0").getFirst().getId());
        assertEquals(pd.getFingerprint(), copy.getFingerprint());
    }

    @Test
    public void testDefinitionsByReference() throws Exception {
        getConfiguration().setStoreDefinitionsByReference(true);

        deploy(bigDefinition("test", 500));

        String key = UUID.randomUUID().toString();
        getEngine().start(key, "test", null);

        byte[] stored = serialize(pm.last);
        assertTrue(stored.length < 5_000, "size: " + stored.length);
        assertTrue(pm.last.getDefinitions().hasReferences());

        getEngine().resume(key, "ev", null);
        assertTrue(getEventManager().find(key, "ev").isEmpty());
    }

    @Test
    public void testChangedDefinitionIsDetected() throws Exception {
        getConfiguration().setStoreDefinitionsByReference(true);

        deploy(bigDefinition("test", 5));

        String key = UUID.randomUUID().toString();
        getEngine().start(key, "test", null);

        // redeploy a different version with the same ID
        deploy(bigDefinition("test", 6));

        ExecutionException e = assertThrows(ExecutionException.class, () -> getEngine().resume(key, "ev", null));
        assertTrue(e.getMessage().contains("was changed"), e.getMessage());

        // the event is kept
        assertEquals(1, getEventManager().find(key, "ev").size());
    }

    /**
     * start --> t0 --> ... --> tN --> ev --> end
     */
    private static ProcessDefinition bigDefinition(String id, int tasks) {
        List<AbstractElement> els = new ArrayList<>();
        els.add(new StartEvent("start"));
        String prev = "start";
        for (int i = 0; i < tasks; i++) {
            els.add(new SequenceFlow("f" + i, prev, "t" + i));
            els.add(new ServiceTask("t" + i));
            prev = "t" + i;
        }
        els.add(new SequenceFlow("fev", prev, "ev"));
        els.add(new IntermediateCatchEvent("ev", "ev"));
        els.add(new SequenceFlow("fend", "ev", "end"));
        els.add(new EndEvent("end"));
        return new ProcessDefinition(id, els);
    }

    private static byte[] serialize(Object o) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(o);
        }
        return bos.toByteArray();
    }

    private static Object deserialize(byte[] ab) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(ab))) {
            return in.readObject();
        }
    }

    private static final class RecordingPersistenceManager extends InMemPersistenceManager {

        private volatile ProcessInstance last;

        @Override
        public void save(ProcessInstance state, long expectedVersion) throws ExecutionException {
            last = state;
            super.save(state, expectedVersion);
        }
    }
}
