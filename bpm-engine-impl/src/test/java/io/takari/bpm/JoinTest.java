package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Join semantics: a join waits for all tokens that can still reach it.
 */
public class JoinTest extends AbstractEngineTest {

    private final AtomicInteger afterJoin = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        afterJoin.set(0);
        getServiceTaskRegistry().register("noop", (JavaDelegate) ctx -> {
        });
        getServiceTaskRegistry().register("after", (JavaDelegate) ctx -> afterJoin.incrementAndGet());
        getServiceTaskRegistry().register("fail", (JavaDelegate) ctx -> {
            throw new BpmnError("e1");
        });
    }

    /**
     * start --> xor --> gw --> end
     *              \
     *               --> end2
     */
    @Test
    public void testExclusiveSplitBeforeParallelGateway() throws Exception {
        for (boolean toGateway : new boolean[]{true, false}) {
            String processId = "test" + toGateway;
            deploy(new ProcessDefinition(processId, Arrays.asList(
                    new StartEvent("start"),
                    new SequenceFlow("f1", "start", "xor"),
                    new ExclusiveGateway("xor"),
                    new SequenceFlow("fa", "xor", "gw", "${" + toGateway + "}"),
                    new SequenceFlow("fb", "xor", "end2"),
                    new EndEvent("end2"),
                    new ParallelGateway("gw"),
                    new SequenceFlow("f2", "gw", "after"),
                    new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                    new SequenceFlow("f3", "after", "end"),
                    new EndEvent("end"))));

            getEngine().start(UUID.randomUUID().toString(), processId, null);
        }

        assertEquals(1, afterJoin.get());
    }

    /**
     * start --> gw1 --> xor --> t1 --> merge --> gw2 --> after --> end
     *              \       \               /    /
     *               \       --> t2 -------     /
     *                --> t3 -------------------
     */
    @Test
    public void testExclusiveSplitInsideParallelBranch() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "xor"),
                new ExclusiveGateway("xor"),
                new SequenceFlow("fa", "xor", "t1", "${true}"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("fa2", "t1", "merge"),
                new SequenceFlow("fb", "xor", "t2", "${false}"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("fb2", "t2", "merge"),
                new ExclusiveGateway("merge"),
                new SequenceFlow("f3", "merge", "gw2"),
                new SequenceFlow("f4", "gw1", "t3"),
                new ServiceTask("t3", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("f5", "t3", "gw2"),
                new ParallelGateway("gw2"),
                new SequenceFlow("f6", "gw2", "after"),
                new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                new SequenceFlow("f7", "after", "end"),
                new EndEvent("end"))));

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        assertEquals(1, afterJoin.get());
        assertActivations(key, processId,
                "start", "f1", "gw1",
                "f2", "xor", "fa", "t1", "fa2", "merge", "f3", "gw2",
                "f4", "t3", "f5", "gw2",
                "f6", "after", "f7", "end");
        assertNoMoreActivations();
    }

    /**
     * The first branch waits in the join, then the second branch leaves through an exclusive gateway.
     * The join must be released.
     * <pre>
     * start --> gw1 --> t1 -------------> gw2 --> after --> end
     *              \                     /
     *               --> xor --> (false) -
     *                      \
     *                       --> end2
     * </pre>
     */
    @Test
    public void testExclusiveSplitReleasesWaitingJoin() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("f3", "t1", "gw2"),
                new SequenceFlow("f4", "gw1", "xor"),
                new ExclusiveGateway("xor"),
                new SequenceFlow("fa", "xor", "gw2", "${false}"),
                new SequenceFlow("fb", "xor", "end2"),
                new EndEvent("end2"),
                new ParallelGateway("gw2"),
                new SequenceFlow("f5", "gw2", "after"),
                new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                new SequenceFlow("f6", "after", "end"),
                new EndEvent("end"))));

        getEngine().start(UUID.randomUUID().toString(), processId, null);

        assertEquals(1, afterJoin.get());
    }

    /**
     * Nested forks, the events are resumed in different orders.
     * <pre>
     * start --> gw1 --> gw2 --> a --> gw3 --> gw4 --> after --> end
     *              \       \         /       /
     *               \       --> b ---       /
     *                --> c -----------------
     * </pre>
     */
    @Test
    public void testNestedForksResumedInAnyOrder() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "gw2"),
                new ParallelGateway("gw2"),
                new SequenceFlow("f3", "gw2", "a"),
                new IntermediateCatchEvent("a", "a"),
                new SequenceFlow("f4", "a", "gw3"),
                new SequenceFlow("f5", "gw2", "b"),
                new IntermediateCatchEvent("b", "b"),
                new SequenceFlow("f6", "b", "gw3"),
                new ParallelGateway("gw3"),
                new SequenceFlow("f7", "gw3", "gw4"),
                new SequenceFlow("f8", "gw1", "c"),
                new IntermediateCatchEvent("c", "c"),
                new SequenceFlow("f9", "c", "gw4"),
                new ParallelGateway("gw4"),
                new SequenceFlow("f10", "gw4", "after"),
                new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                new SequenceFlow("f11", "after", "end"),
                new EndEvent("end"))));

        String[][] orders = {
                {"a", "b", "c"}, {"a", "c", "b"}, {"b", "a", "c"},
                {"b", "c", "a"}, {"c", "a", "b"}, {"c", "b", "a"}};

        for (String[] order : orders) {
            String key = UUID.randomUUID().toString();
            getEngine().start(key, processId, null);
            for (String ev : order) {
                assertEquals(0, afterJoin.get(), "order: " + Arrays.toString(order));
                getEngine().resume(key, ev, null);
            }
            assertEquals(1, afterJoin.getAndSet(0), "order: " + Arrays.toString(order));
        }
    }

    /**
     * An inclusive fork/join inside a loop.
     * <pre>
     * start --> inc --> gw1 --> t1 --> gw2 --> xor --> after --> end
     *           ^          \          /          /
     *           |           --> t2 ---          /
     *           ------------------------- (loop)
     * </pre>
     */
    @Test
    public void testInclusiveForkJoinInLoop() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        getServiceTaskRegistry().register("inc", (JavaDelegate) ctx -> ctx.setVariable("cnt", counter.incrementAndGet()));

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "inc"),
                new ServiceTask("inc", ExpressionType.DELEGATE, "${inc}"),
                new SequenceFlow("f2", "inc", "gw1"),
                new InclusiveGateway("gw1"),
                new SequenceFlow("f3", "gw1", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("f4", "t1", "gw2"),
                new SequenceFlow("f5", "gw1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("f6", "t2", "gw2"),
                new InclusiveGateway("gw2"),
                new SequenceFlow("f7", "gw2", "xor"),
                new ExclusiveGateway("xor", "f8"),
                new SequenceFlow("f8", "xor", "inc"),
                new SequenceFlow("f9", "xor", "after", "${cnt >= 5}"),
                new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                new SequenceFlow("f10", "after", "end"),
                new EndEvent("end"))));

        getEngine().start(UUID.randomUUID().toString(), processId, null);

        assertEquals(5, counter.get());
        assertEquals(1, afterJoin.get());
    }

    /**
     * One branch waits in the join, the other one selects an event of an event-based gateway that
     * doesn't lead to the join.
     * <pre>
     * start --> gw1 --> t1 --------------> gw2 --> after --> end
     *              \                      /
     *               --> evgw --> ev1 -----
     *                       \
     *                        --> ev2 --> end2
     * </pre>
     */
    @Test
    public void testEventGatewayReleasesWaitingJoin() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("f3", "t1", "gw2"),
                new SequenceFlow("f4", "gw1", "evgw"),
                new EventBasedGateway("evgw"),
                new SequenceFlow("f5", "evgw", "ev1"),
                new IntermediateCatchEvent("ev1", "ev1"),
                new SequenceFlow("f6", "ev1", "gw2"),
                new SequenceFlow("f7", "evgw", "ev2"),
                new IntermediateCatchEvent("ev2", "ev2"),
                new SequenceFlow("f8", "ev2", "end2"),
                new EndEvent("end2"),
                new ParallelGateway("gw2"),
                new SequenceFlow("f9", "gw2", "after"),
                new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                new SequenceFlow("f10", "after", "end"),
                new EndEvent("end"))));

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);
        assertEquals(0, afterJoin.get());

        getEngine().resume(key, "ev2", null);
        assertEquals(1, afterJoin.get());
    }

    /**
     * A task in a parallel branch fails, its boundary error event leads to the join.
     * <pre>
     * start --> gw1 --> t1 -----------> gw2 --> after --> end
     *              \      \            / /
     *               \      --> (err) --  /
     *                --> t2 ------------
     * </pre>
     */
    @Test
    public void testBoundaryErrorIntoJoin() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${fail}"),
                new BoundaryEvent("be", "t1", "e1"),
                new SequenceFlow("fbe", "be", "gw2"),
                new SequenceFlow("f3", "t1", "gw2"),
                new SequenceFlow("f4", "gw1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("f5", "t2", "gw2"),
                new ParallelGateway("gw2"),
                new SequenceFlow("f6", "gw2", "after"),
                new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                new SequenceFlow("f7", "after", "end"),
                new EndEvent("end"))));

        getEngine().start(UUID.randomUUID().toString(), processId, null);

        assertEquals(1, afterJoin.get());
    }

    /**
     * A task in a parallel branch fails, its boundary error event leads to a separate end event.
     * The join must not wait for the failed branch.
     */
    @Test
    public void testBoundaryErrorAwayFromJoin() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${noop}"),
                new SequenceFlow("f3", "t2", "gw2"),
                new SequenceFlow("f4", "gw1", "t1"),
                new ServiceTask("t1", ExpressionType.DELEGATE, "${fail}"),
                new BoundaryEvent("be", "t1", "e1"),
                new SequenceFlow("fbe", "be", "end2"),
                new EndEvent("end2"),
                new SequenceFlow("f5", "t1", "gw2"),
                new ParallelGateway("gw2"),
                new SequenceFlow("f6", "gw2", "after"),
                new ServiceTask("after", ExpressionType.DELEGATE, "${after}"),
                new SequenceFlow("f7", "after", "end"),
                new EndEvent("end"))));

        getEngine().start(UUID.randomUUID().toString(), processId, null);

        assertEquals(1, afterJoin.get());
    }
}
