package io.takari.bpm;

import io.takari.bpm.api.Engine;
import io.takari.bpm.model.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DefinitionLookupTest {

    @Test
    public void testStartUsesTheConfiguredDefinitionProvider() throws Exception {
        ProcessDefinition pd = new ProcessDefinition("test", Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end")));

        AtomicInteger loads = new AtomicInteger();
        ProcessDefinitionProvider provider = id -> {
            loads.incrementAndGet();
            return pd;
        };

        Engine engine = new EngineBuilder()
                .withDefinitionProvider(provider)
                .withTaskRegistry(new io.takari.bpm.task.ServiceTaskRegistryImpl())
                .wrapDefinitionProviderWith(CachingIndexedProcessDefinitionProvider::new)
                .build();

        for (int i = 0; i < 3; i++) {
            engine.start(UUID.randomUUID().toString(), "test", null);
        }

        assertEquals(1, loads.get());
    }

    @Test
    public void testIndexes() throws Exception {
        IndexedProcessDefinition pd = new IndexedProcessDefinition(new ProcessDefinition("test", Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new ParallelGateway("gw"),
                new SequenceFlow("f2", "gw", "sub"),
                new SequenceFlow("f3", "gw", "t1"),
                new SubProcess("sub", Arrays.asList(
                        new StartEvent("sstart"),
                        new SequenceFlow("s1", "sstart", "send"),
                        new EndEvent("send"))),
                new ServiceTask("t1"),
                new SequenceFlow("f4", "sub", "end"),
                new SequenceFlow("f5", "t1", "end"),
                new EndEvent("end"))));

        assertEquals(List.of("f2", "f3"), ProcessDefinitionUtils.toIds(pd.findOptionalOutgoingFlows("gw")));
        assertEquals(List.of("f4", "f5"), ProcessDefinitionUtils.toIds(ProcessDefinitionUtils.findIncomingFlows(pd, "end")));
        assertEquals(List.of("s1"), ProcessDefinitionUtils.toIds(ProcessDefinitionUtils.findIncomingFlows(pd, "send")));
        assertTrue(pd.findOptionalOutgoingFlows("end").isEmpty());
        assertNull(pd.findOptionalOutgoingFlows("unknown"));

        assertInstanceOf(EndEvent.class, ProcessDefinitionUtils.findElement(pd, "send"));
        assertEquals("sub", ProcessDefinitionUtils.findElementProcess(pd, "send").getId());
        assertEquals("test", ProcessDefinitionUtils.findElementProcess(pd, "sub").getId());
        assertThrows(Exception.class, () -> ProcessDefinitionUtils.findElement(pd, "unknown"));

        // the reachable joins are computed once
        assertSame(ProcessDefinitionUtils.findReachableJoinFlows(pd, "start"), ProcessDefinitionUtils.findReachableJoinFlows(pd, "start"));
        assertEquals(1, ProcessDefinitionUtils.findReachableJoinFlows(pd, "start").get("f1"));
    }

    /**
     * Indexing used to be O(n^2).
     */
    @Test
    public void testIndexingLargeDefinitions() {
        List<AbstractElement> els = new ArrayList<>();
        els.add(new StartEvent("start"));
        String prev = "start";
        for (int i = 0; i < 50_000; i++) {
            els.add(new SequenceFlow("f" + i, prev, "t" + i));
            els.add(new ServiceTask("t" + i));
            prev = "t" + i;
        }
        els.add(new SequenceFlow("fend", prev, "end"));
        els.add(new EndEvent("end"));
        ProcessDefinition pd = new ProcessDefinition("big", els);

        IndexedProcessDefinition ipd = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> new IndexedProcessDefinition(pd));
        assertEquals("f49999", ProcessDefinitionUtils.toIds(ipd.findOptionalOutgoingFlows("t49998")).getFirst());
    }
}
