package io.takari.bpm.model;

import io.takari.bpm.model.ProcessDefinitionBuilder.Seq;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessDefinitionBuilderTest {

    private static Set<VariableMapping> vars1 = new LinkedHashSet<>();
    private static Set<VariableMapping> vars2 = new LinkedHashSet<>();

    @Test
    public void testProcess() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .apply(ProcessDefinitionBuilderTest::fillSeq)
                .end();
        assertElements(def.getChildren());
    }

    @Test
    public void testSub() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .sub()
                    .apply(ProcessDefinitionBuilderTest::fillSeq)
                    .end()
                .sub(true)
                    .end()
                .sub(true, Collections.singleton(new VariableMapping("src", "srcExpr", "target")))
                    .end()
                .end();

        StartEvent s;
        SubProcess sp;

        Iterator<AbstractElement> it = def.getChildren().iterator();
        s = validateStart(it);
        sp = validateElemFlow(it, SubProcess.class, s);
        assertFalse(sp.isUseSeparateContext());
        assertElements(sp.getChildren());
        sp = validateElemFlow(it, SubProcess.class, sp);
        assertTrue(sp.isUseSeparateContext());
        assertTrue(sp.getOutVariables().isEmpty());
        sp = validateElemFlow(it, SubProcess.class, sp);
        assertTrue(sp.isUseSeparateContext());
        assertEquals(Collections.singleton(new VariableMapping("src", "srcExpr", "target")), sp.getOutVariables());
        validateEndFlow(it, sp);
        assertFalse(it.hasNext());

    }

    @Test
    public void testForkEnd() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .task("t1")
                .fork()
                    .task("t2")
                    .end("err")
                .fork()
                    .task("t3")
                    .end("err", "cause")
                .task("t4")
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();

        StartEvent s;
        ServiceTask t, t1;

        s = validateStart(it);
        t = t1 = validateTaskFlow(it, "t1", s);
        t = validateTaskFlow(it, "t2", t1);
        validateEndFlow(it, t, "err");

        t = validateTaskFlow(it, "t3", t1);
        validateEndFlow(it, t, "err", "cause");

        t = validateTaskFlow(it, "t4", t1);
        validateEndFlow(it, t);

        assertFalse(it.hasNext());
    }

    @Test
    public void testForkJoinAll() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .task("t1")
                .fork()
                    .task("t2")
                    .joinTo("join1")
                .fork()
                    .task("t3")
                    .joinTo("join1")
                .joinAll("join1")
                .task("t4")
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();

        StartEvent s;
        ServiceTask t, t1, t2, t3;

        s = validateStart(it);
        t = t1 = validateTaskFlow(it, "t1", s);
        t = t2 = validateTaskFlow(it, "t2", t1);
        t = t3 = validateTaskFlow(it, "t3", t1);

        String ft2 = validateFlow(it, t2);
        String ft3 = validateFlow(it, t3);
        assertTrue(ft2.equals(ft3));
        t = validateTask(it, "t4", ft2);

        validateEndFlow(it, t);

        assertFalse(it.hasNext());
    }

    @Test
    public void testForkJoinPoint() {
        // [start] -> t1 -> t2 -> t4, t1 -> t3 -> t4, t4 -> t5 -> t4, t4 -> [end]
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .task("t1")
                .fork()
                    .task("t2")
                    .joinTo("join2")
                .task("t3")
                .joinPoint("join2")
                .task("t4")
                .fork()
                    .task("t5")
                    .joinTo("join2")
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();

        StartEvent s;
        ServiceTask t1, t2, t3, t4, t5;

        s = validateStart(it);
        t1 = validateTaskFlow(it, "t1", s);
        t2 = validateTaskFlow(it, "t2", t1);
        t3 = validateTaskFlow(it, "t3", t1);

        String ft2 = validateFlow(it, t2);
        String ft3 = validateFlow(it, t3);
        assertTrue(ft2.equals(ft3));
        t4 = validateTask(it, "t4", ft2);

        t5 = validateTaskFlow(it, "t5", t4);
        String ft5 = validateFlow(it, t5);
        assertEquals(t4.getId(), ft5);

        validateEndFlow(it, t4);

        assertFalse(it.hasNext());
    }

    @Test
    public void testForkJoinEnd() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .task("t1")
                .fork()
                    .task("t2")
                    .joinTo("join2")
                .task("t3")
                .joinPoint("join2")
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();
        StartEvent s;
        ServiceTask t1, t2, t3;

        s = validateStart(it);
        t1 = validateTaskFlow(it, "t1", s);
        t2 = validateTaskFlow(it, "t2", t1);
        t3 = validateTaskFlow(it, "t3", t1);

        String ft2 = validateFlow(it, t2);
        String ft3 = validateFlow(it, t3);
        assertTrue(ft2.equals(ft3));

        validateEnd(it, ft2);

        assertFalse(it.hasNext());
    }

    @Test
    public void testForkLoop() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .task("t1")
                .fork()
                    .task("t2")
                    .loop()
                .fork()
                    .task("t3")
                    .loop()
                .task("t4")
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();

        StartEvent s;
        ServiceTask t, t1, t2, t3;

        s = validateStart(it);
        t = t1 = validateTaskFlow(it, "t1", s);
        t = t2 = validateTaskFlow(it, "t2", t1);
        t = t3 = validateTaskFlow(it, "t3", t1);

        String ft2 = validateFlow(it, t2);
        String ft3 = validateFlow(it, t3);
        String ft1 = validateFlow(it, t1);
        assertTrue(ft1.equals(ft2));
        assertTrue(ft2.equals(ft3));
        t = validateTask(it, "t4", ft1);

        validateEndFlow(it, t);

        assertFalse(it.hasNext());
    }

    @Test
    public void testTieForks() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .task("t1")
                .fork()
                    .task("t2")
                    .loop()
                .fork()
                    .task("t3")
                    .loop()
                .tieForks()
                .task("t4")
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();

        StartEvent s;
        ServiceTask t, t1, t2, t3;

        s = validateStart(it);
        t = t1 = validateTaskFlow(it, "t1", s);
        t = t2 = validateTaskFlow(it, "t2", t1);
        t = t3 = validateTaskFlow(it, "t3", t1);

        String ft2 = validateFlow(it, t2);
        String ft3 = validateFlow(it, t3);
        assertTrue(ft2.equals(ft3));
        t = validateTask(it, "t4", ft2);

        validateEndFlow(it, t);

        assertFalse(it.hasNext());
    }

    @Test
    public void testForkLoopWithExpressions() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .task("t1")
                .fork()
                    .task("t2")
                    .flowExpr("foo")
                    .loop()
                .fork()
                    .task("t3")
                    .flowExpr("bar")
                    .loop()
                .flowExpr("baz")
                .task("t4")
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();

        StartEvent s;
        ServiceTask t, t1, t2, t3;

        s = validateStart(it);
        t = t1 = validateTaskFlow(it, "t1", s);
        t = t2 = validateTaskFlow(it, "t2", t1);
        t = t3 = validateTaskFlow(it, "t3", t1);

        String ft2 = validateFlow(it, t2, null, "foo");
        String ft3 = validateFlow(it, t3, null, "bar");
        String ft1 = validateFlow(it, t1, null, "baz");
        assertTrue(ft1.equals(ft2));
        assertTrue(ft2.equals(ft3));
        t = validateTask(it, "t4", ft1);

        validateEndFlow(it, t);

        assertFalse(it.hasNext());
    }

    @Test
    public void testApply() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .apply(s -> s.task("t1"))
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();
        StartEvent s;
        ServiceTask t;

        s = validateStart(it);
        t = validateTaskFlow(it, "t1", s);
        validateEndFlow(it, t);

        assertFalse(it.hasNext());
    }

    @Test
    public void testApplyEach() {
        ProcessDefinition def = ProcessDefinitionBuilder.newProcess("test")
                .applyEach(Arrays.asList("t1", "t2", "t3"), (s, e) ->
                        s.fork()
                                .task(e)
                                .joinTo("join")
                )
                .joinAll("join")
                .end();

        Iterator<AbstractElement> it = def.getChildren().iterator();
        StartEvent s;
        ServiceTask t1, t2, t3;

        s = validateStart(it);
        t1 = validateTaskFlow(it, "t1", s);
        t2 = validateTaskFlow(it, "t2", s);
        t3 = validateTaskFlow(it, "t3", s);
        String ft1 = validateFlow(it, t1);
        String ft2 = validateFlow(it, t2);
        String ft3 = validateFlow(it, t3);
        assertTrue(ft1.equals(ft2));
        assertTrue(ft2.equals(ft3));

        validateEnd(it, ft1);

        assertFalse(it.hasNext());
    }

    private static Seq fillSeq(Seq seq) {
        return seq
                .call("c1")
                .call("c2", true)
                .call("c3", vars1, vars2)
                .call("c4", vars1, vars2, true)
                .catchEvent()
                .catchEvent("catch2")
                .catchEvent("catch3", "time3", "dur3")
                .catchEventExpr("catch4")
                .catchEventExpr("catch5", "time5", "dur5")
                .eventGate()
                .exclusiveGate()
                .exclusiveGate("excl")
                .inclusiveGate()
                .parallelGate()
                .task("t1")
                .task(ExpressionType.NONE, "t2")
                .taskDelegate("t3")
                .boundaryEvent()
                    .end()
                .boundaryEvent("errorRef")
                    .end()
                .boundaryEvent("errorRef", "timeDuration")
                    .end();
    }

    private static void assertElements(Collection<AbstractElement> elems) {
        Iterator<AbstractElement> it = elems.iterator();

        StartEvent s;
        CallActivity call;
        IntermediateCatchEvent ct;
        EventBasedGateway eg;
        ExclusiveGateway xg;
        InclusiveGateway ig;
        ParallelGateway pg;
        ServiceTask t;

        s = validateStart(it);


        call = validateElemFlow(it, CallActivity.class, s);
        assertEquals("c1", call.getCalledElement());
        assertFalse(call.isCopyAllVariables());
        assertNull(call.getIn());
        assertNull(call.getOut());

        call = validateElemFlow(it, CallActivity.class, call);
        assertEquals("c2", call.getCalledElement());
        assertTrue(call.isCopyAllVariables());
        assertNull(call.getIn());
        assertNull(call.getOut());

        call = validateElemFlow(it, CallActivity.class, call);
        assertEquals("c3", call.getCalledElement());
        assertFalse(call.isCopyAllVariables());
        assertSame(vars1, call.getIn());
        assertSame(vars2, call.getOut());

        call = validateElemFlow(it, CallActivity.class, call);
        assertEquals("c4", call.getCalledElement());
        assertTrue(call.isCopyAllVariables());
        assertSame(vars1, call.getIn());
        assertSame(vars2, call.getOut());


        ct = validateElemFlow(it, IntermediateCatchEvent.class, call);
        assertNull(ct.getMessageRef());
        assertNull(ct.getMessageRefExpression());
        assertNull(ct.getTimeDate());
        assertNull(ct.getTimeDuration());

        ct = validateElemFlow(it, IntermediateCatchEvent.class, ct);
        assertEquals("catch2", ct.getMessageRef());
        assertNull(ct.getMessageRefExpression());
        assertNull(ct.getTimeDate());
        assertNull(ct.getTimeDuration());

        ct = validateElemFlow(it, IntermediateCatchEvent.class, ct);
        assertEquals("catch3", ct.getMessageRef());
        assertNull(ct.getMessageRefExpression());
        assertEquals("time3", ct.getTimeDate());
        assertEquals("dur3", ct.getTimeDuration());

        ct = validateElemFlow(it, IntermediateCatchEvent.class, ct);
        assertNull(ct.getMessageRef());
        assertEquals("catch4", ct.getMessageRefExpression());
        assertNull(ct.getTimeDate());
        assertNull(ct.getTimeDuration());

        ct = validateElemFlow(it, IntermediateCatchEvent.class, ct);
        assertNull(ct.getMessageRef());
        assertEquals("catch5", ct.getMessageRefExpression());
        assertEquals("time5", ct.getTimeDate());
        assertEquals("dur5", ct.getTimeDuration());


        eg = validateElemFlow(it, EventBasedGateway.class, ct);

        xg = validateElemFlow(it, ExclusiveGateway.class, eg);
        assertNull(xg.getDefaultFlow());

        xg = validateElemFlow(it, ExclusiveGateway.class, xg);
        assertEquals("excl", xg.getDefaultFlow());

        ig = validateElemFlow(it, InclusiveGateway.class, xg);

        pg = validateElemFlow(it, ParallelGateway.class, ig);


        t = validateElemFlow(it, ServiceTask.class, pg);
        assertEquals("t1", t.getExpression());
        assertEquals(ExpressionType.SIMPLE, t.getType());

        t = validateElemFlow(it, ServiceTask.class, t);
        assertEquals("t2", t.getExpression());
        assertEquals(ExpressionType.NONE, t.getType());

        t = validateElemFlow(it, ServiceTask.class, t);
        assertEquals("t3", t.getExpression());
        assertEquals(ExpressionType.DELEGATE, t.getType());

        BoundaryEvent be;
        be = validateBoundaryEvent(it, t);
        assertNull(be.getErrorRef());
        assertNull(be.getTimeDuration());
        validateEndFlow(it, be);

        be = validateBoundaryEvent(it, t);
        assertEquals("errorRef", be.getErrorRef());
        assertNull(be.getTimeDuration());
        validateEndFlow(it, be);

        be = validateBoundaryEvent(it, t);
        assertEquals("errorRef", be.getErrorRef());
        assertEquals("timeDuration", be.getTimeDuration());
        validateEndFlow(it, be);

        validateEndFlow(it, t);

        assertFalse(it.hasNext());
    }

    private static StartEvent validateStart(Iterator<AbstractElement> it) {
        assertTrue(it.hasNext());
        AbstractElement elem = it.next();
        assertInstanceOf(StartEvent.class, elem);
        return (StartEvent) elem;
    }

    private static void validateEndFlow(Iterator<AbstractElement> it, AbstractElement prev) {
        validateEndFlow(it, prev, null);
    }

    private static void validateEndFlow(Iterator<AbstractElement> it, AbstractElement prev, String errorRef) {
        validateEndFlow(it, prev, errorRef, null);
    }

    private static void validateEndFlow(Iterator<AbstractElement> it, AbstractElement prev, String errorRef, String causeExpression) {
        validateEnd(it, validateFlow(it, prev), errorRef, causeExpression);
    }

    private static void validateEnd(Iterator<AbstractElement> it, String id) {
        validateEnd(it, id, null, null);
    }

    private static void validateEnd(Iterator<AbstractElement> it, String id, String errorRef, String causeExpression) {
        EndEvent e = validateElem(it, EndEvent.class, id);
        assertEquals(errorRef, e.getErrorRef());
        assertEquals(causeExpression, e.getCauseExpression());
    }

    private static ServiceTask validateTaskFlow(Iterator<AbstractElement> it, String expr, AbstractElement prev) {
        ServiceTask t = validateElemFlow(it, ServiceTask.class, prev);
        assertEquals(expr, t.getExpression());
        return t;
    }

    private static ServiceTask validateTask(Iterator<AbstractElement> it, String expr, String id) {
        ServiceTask t = validateElem(it, ServiceTask.class, id);
        assertEquals(expr, t.getExpression());
        return t;
    }

    private static <T extends AbstractElement> T validateElemFlow(Iterator<AbstractElement> it, Class<T> cl, AbstractElement prev) {
        return validateElem(it, cl, validateFlow(it, prev));
    }

    private static <T extends AbstractElement> T validateElem(Iterator<AbstractElement> it, Class<T> cl, String id) {

        assertTrue(it.hasNext());
        AbstractElement elem = it.next();
        assertInstanceOf(cl, elem);
        if (id != null) {
            assertEquals(id, elem.getId());
        }
        return cl.cast(elem);
    }

    private static String validateFlow(Iterator<AbstractElement> it, AbstractElement prev) {
        return validateFlow(it, prev, null, null);
    }

    private static String validateFlow(Iterator<AbstractElement> it, AbstractElement prev, String flowName, String flowExpr) {
        assertNotNull(prev);
        assertTrue(it.hasNext());

        AbstractElement elem = it.next();
        assertInstanceOf(SequenceFlow.class, elem);
        SequenceFlow f = (SequenceFlow) elem;
        assertEquals(prev.getId(), f.getFrom());
        if(flowName != null) {
            assertEquals(flowName, f.getId());
        }
        assertEquals(flowExpr, f.getExpression());

        return f.getTo();
    }

    private static BoundaryEvent validateBoundaryEvent(Iterator<AbstractElement> it, AbstractElement prev) {
        assertNotNull(prev);
        assertTrue(it.hasNext());

        AbstractElement elem = it.next();
        assertInstanceOf(BoundaryEvent.class, elem);
        BoundaryEvent be = (BoundaryEvent) elem;
        assertEquals(prev.getId(), be.getAttachedToRef());

        return be;
    }
}
