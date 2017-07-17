package io.takari.bpm.model;

import io.takari.bpm.model.ProcessDefinitionBuilder.Seq;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

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
        assertThat(sp.isUseSeparateContext(), is(false));
        assertElements(sp.getChildren());
        sp = validateElemFlow(it, SubProcess.class, sp);
        assertThat(sp.isUseSeparateContext(), is(true));
        assertThat(sp.getOutVariables().isEmpty(), is(true));
        sp = validateElemFlow(it, SubProcess.class, sp);
        assertThat(sp.isUseSeparateContext(), is(true));
        assertThat(sp.getOutVariables(), is(Collections.singleton(new VariableMapping("src", "srcExpr", "target"))));
        validateEndFlow(it, sp);
        assertThat(it.hasNext(), is(false));

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

        assertThat(it.hasNext(), is(false));
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
        assertThat(ft2.equals(ft3), is(true));
        t = validateTask(it, "t4", ft2);

        validateEndFlow(it, t);

        assertThat(it.hasNext(), is(false));
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
        assertThat(ft2.equals(ft3), is(true));
        t4 = validateTask(it, "t4", ft2);

        t5 = validateTaskFlow(it, "t5", t4);
        String ft5 = validateFlow(it, t5);
        assertThat(ft5, is(t4.getId()));

        validateEndFlow(it, t4);

        assertThat(it.hasNext(), is(false));
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
        assertThat(ft2.equals(ft3), is(true));

        validateEnd(it, ft2);

        assertThat(it.hasNext(), is(false));
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
        assertThat(ft1.equals(ft2), is(true));
        assertThat(ft2.equals(ft3), is(true));
        t = validateTask(it, "t4", ft1);

        validateEndFlow(it, t);

        assertThat(it.hasNext(), is(false));
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
        assertThat(ft1.equals(ft2), is(true));
        assertThat(ft2.equals(ft3), is(true));
        t = validateTask(it, "t4", ft1);

        validateEndFlow(it, t);

        assertThat(it.hasNext(), is(false));
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

        assertThat(it.hasNext(), is(false));
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
        assertThat(ft1.equals(ft2), is(true));
        assertThat(ft2.equals(ft3), is(true));

        validateEnd(it, ft1);

        assertThat(it.hasNext(), is(false));
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
        assertThat(call.getCalledElement(), is("c1"));
        assertThat(call.isCopyAllVariables(), is(false));
        assertThat(call.getIn(), nullValue());
        assertThat(call.getOut(), nullValue());

        call = validateElemFlow(it, CallActivity.class, call);
        assertThat(call.getCalledElement(), is("c2"));
        assertThat(call.isCopyAllVariables(), is(true));
        assertThat(call.getIn(), nullValue());
        assertThat(call.getOut(), nullValue());

        call = validateElemFlow(it, CallActivity.class, call);
        assertThat(call.getCalledElement(), is("c3"));
        assertThat(call.isCopyAllVariables(), is(false));
        assertThat(call.getIn(), sameInstance(vars1));
        assertThat(call.getOut(), sameInstance(vars2));

        call = validateElemFlow(it, CallActivity.class, call);
        assertThat(call.getCalledElement(), is("c4"));
        assertThat(call.isCopyAllVariables(), is(true));
        assertThat(call.getIn(), sameInstance(vars1));
        assertThat(call.getOut(), sameInstance(vars2));


        ct = validateElemFlow(it, IntermediateCatchEvent.class, call);
        assertThat(ct.getMessageRef(), nullValue());
        assertThat(ct.getMessageRefExpression(), nullValue());
        assertThat(ct.getTimeDate(), nullValue());
        assertThat(ct.getTimeDuration(), nullValue());

        ct = validateElemFlow(it, IntermediateCatchEvent.class, ct);
        assertThat(ct.getMessageRef(), is("catch2"));
        assertThat(ct.getMessageRefExpression(), nullValue());
        assertThat(ct.getTimeDate(), nullValue());
        assertThat(ct.getTimeDuration(), nullValue());

        ct = validateElemFlow(it, IntermediateCatchEvent.class, ct);
        assertThat(ct.getMessageRef(), is("catch3"));
        assertThat(ct.getMessageRefExpression(), nullValue());
        assertThat(ct.getTimeDate(), is("time3"));
        assertThat(ct.getTimeDuration(), is("dur3"));

        ct = validateElemFlow(it, IntermediateCatchEvent.class, ct);
        assertThat(ct.getMessageRef(), nullValue());
        assertThat(ct.getMessageRefExpression(), is("catch4"));
        assertThat(ct.getTimeDate(), nullValue());
        assertThat(ct.getTimeDuration(), nullValue());

        ct = validateElemFlow(it, IntermediateCatchEvent.class, ct);
        assertThat(ct.getMessageRef(), nullValue());
        assertThat(ct.getMessageRefExpression(), is("catch5"));
        assertThat(ct.getTimeDate(), is("time5"));
        assertThat(ct.getTimeDuration(), is("dur5"));


        eg = validateElemFlow(it, EventBasedGateway.class, ct);

        xg = validateElemFlow(it, ExclusiveGateway.class, eg);
        assertThat(xg.getDefaultFlow(), nullValue());

        xg = validateElemFlow(it, ExclusiveGateway.class, xg);
        assertThat(xg.getDefaultFlow(), is("excl"));

        ig = validateElemFlow(it, InclusiveGateway.class, xg);

        pg = validateElemFlow(it, ParallelGateway.class, ig);


        t = validateElemFlow(it, ServiceTask.class, pg);
        assertThat(t.getExpression(), is("t1"));
        assertThat(t.getType(), is(ExpressionType.SIMPLE));

        t = validateElemFlow(it, ServiceTask.class, t);
        assertThat(t.getExpression(), is("t2"));
        assertThat(t.getType(), is(ExpressionType.NONE));

        t = validateElemFlow(it, ServiceTask.class, t);
        assertThat(t.getExpression(), is("t3"));
        assertThat(t.getType(), is(ExpressionType.DELEGATE));

        BoundaryEvent be;
        be = validateBoundaryEvent(it, t);
        assertThat(be.getErrorRef(), nullValue());
        assertThat(be.getTimeDuration(), nullValue());
        validateEndFlow(it, be);

        be = validateBoundaryEvent(it, t);
        assertThat(be.getErrorRef(), is("errorRef"));
        assertThat(be.getTimeDuration(), nullValue());
        validateEndFlow(it, be);

        be = validateBoundaryEvent(it, t);
        assertThat(be.getErrorRef(), is("errorRef"));
        assertThat(be.getTimeDuration(), is("timeDuration"));
        validateEndFlow(it, be);

        validateEndFlow(it, t);

        assertThat(it.hasNext(), is(false));
    }

    private static StartEvent validateStart(Iterator<AbstractElement> it) {
        assertThat(it.hasNext(), is(true));
        AbstractElement elem = it.next();
        assertThat(elem, instanceOf(StartEvent.class));
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
        assertThat(e.getErrorRef(), is(errorRef));
        assertThat(e.getCauseExpression(), is(causeExpression));
    }

    private static ServiceTask validateTaskFlow(Iterator<AbstractElement> it, String expr, AbstractElement prev) {
        ServiceTask t = validateElemFlow(it, ServiceTask.class, prev);
        assertThat(t.getExpression(), is(expr));
        return t;
    }

    private static ServiceTask validateTask(Iterator<AbstractElement> it, String expr, String id) {
        ServiceTask t = validateElem(it, ServiceTask.class, id);
        assertThat(t.getExpression(), is(expr));
        return t;
    }

    private static <T extends AbstractElement> T validateElemFlow(Iterator<AbstractElement> it, Class<T> cl, AbstractElement prev) {
        return validateElem(it, cl, validateFlow(it, prev));
    }

    private static <T extends AbstractElement> T validateElem(Iterator<AbstractElement> it, Class<T> cl, String id) {

        assertThat(it.hasNext(), is(true));
        AbstractElement elem = it.next();
        assertThat(elem, instanceOf(cl));
        if (id != null) {
            assertThat(elem.getId(), is(id));
        }
        return cl.cast(elem);
    }

    private static String validateFlow(Iterator<AbstractElement> it, AbstractElement prev) {
        return validateFlow(it, prev, null, null);
    }

    private static String validateFlow(Iterator<AbstractElement> it, AbstractElement prev, String flowName, String flowExpr) {
        assertThat(prev, notNullValue());
        assertThat(it.hasNext(), is(true));

        AbstractElement elem = it.next();
        assertThat(elem, instanceOf(SequenceFlow.class));
        SequenceFlow f = (SequenceFlow) elem;
        assertThat(f.getFrom(), is(prev.getId()));
        if(flowName != null) {
            assertThat(f.getId(), is(flowName));
        }
        assertThat(f.getExpression(), is(flowExpr));

        return f.getTo();
    }

    private static BoundaryEvent validateBoundaryEvent(Iterator<AbstractElement> it, AbstractElement prev) {
        assertThat(prev, notNullValue());
        assertThat(it.hasNext(), is(true));

        AbstractElement elem = it.next();
        assertThat(elem, instanceOf(BoundaryEvent.class));
        BoundaryEvent be = (BoundaryEvent) elem;
        assertThat(be.getAttachedToRef(), is(prev.getId()));

        return be;
    }
}
