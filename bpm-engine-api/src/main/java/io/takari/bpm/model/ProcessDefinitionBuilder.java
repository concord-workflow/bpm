package io.takari.bpm.model;

import io.takari.bpm.model.SourceMap.Significance;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ProcessDefinitionBuilder {

    /**
     * Creates a new ProcessBuilder which will return a fully built SourceAwareProcessDefinition when complete
     */
    public static Process newProcess(String id) {
        return new ProcessImpl(id);
    }

    /**
     * Basic builder which allows a number of elements to be added in sequence. This builder is not generified to allow simplified usage.
     */
    public interface Seq {

        /**
         * Adds an arbitrary element given a stepId
         */
        Seq add(Function<String, AbstractElement> elemFun);

        /**
         * Adds a ServiceTask element
         */
        Seq task(ExpressionType expType, String expr);

        /**
         * Adds a ServiceTask element with in/out mappings
         */
        Seq task(ExpressionType expType, String expr, Set<VariableMapping> in, Set<VariableMapping> out);

        /**
         * Adds a ServiceTask element with in/out mappings
         */
        Seq task(ExpressionType expType, String expr, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables);

        /**
         * Adds a simple ServiceTask element
         */
        Seq task(String expr);

        /**
         * Adds a simple ServiceTask with in/out mappings
         */
        Seq task(String expr, Set<VariableMapping> in, Set<VariableMapping> out);

        /**
         * Adds a simple ServiceTask with in/out mappings
         */
        Seq task(String expr, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables);

        /**
         * Adds a delegate ServiceTask element
         */
        Seq taskDelegate(String expr);

        Seq userTask(List<UserTask.Extension> extensions);

        /**
         * Adds a ScriptTask element.
         */
        Seq script(ScriptTask.Type type, String language, String content);

        /**
         * Adds a ScriptTask element.
         */
        Seq script(ScriptTask.Type type, String language, String content, boolean copyAllVariables);

        /**
         * Adds a CallActivity element
         */
        Seq call(String calledElement);

        /**
         * Adds a CallActivity element
         */
        Seq call(String calledElement, boolean copyAllVariables);

        /**
         * Adds a CallActivity element
         */
        Seq call(String calledElement, Set<VariableMapping> in, Set<VariableMapping> out);

        /**
         * Adds a CallActivity element
         */
        Seq call(String calledElement, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables);
        
        /**
         * Adds a CallActivity element
         */
        Seq call(String calledElement, String calledElementExpr, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables);

        /**
         * Adds an EventBasedGateway element
         */
        Seq eventGate();

        /**
         * Adds a ParallelGateway element
         */
        Seq parallelGate();

        /**
         * Adds an InclusiveGateway element
         */
        Seq inclusiveGate();

        /**
         * Adds an ExclusiveGateway element
         */
        Seq exclusiveGate();

        /**
         * Adds an ExclusiveGateway element
         */
        Seq exclusiveGate(String defaultFlow);

        /**
         * Adds an explicit named flow to be used by exclusive gate
         */
        Seq flow(String name);

        /**
         * Adds an explicit flow to be used by exclusive gate when the expression evaluates to true
         */
        Seq flowExpr(String expression);

        /**
         * Adds an explicit named flow to be used by exclusive gate when the expression evaluates to true
         */
        Seq flowExpr(String name, String expression);

        /**
         * Adds an IntermediateCatchEvent element
         */
        Seq catchEvent();

        /**
         * Adds an IntermediateCatchEvent element
         */
        Seq catchEvent(String messageRef);

        /**
         * Adds an IntermediateCatchEvent element with a messageRef expression
         */
        Seq catchEventExpr(String messageRefExpr);

        /**
         * Adds an IntermediateCatchEvent element
         */
        Seq catchEvent(String messageRef, String timeDate, String timeDuration);

        /**
         * Adds an IntermediateCatchEvent element with a messageRef expression
         */
        Seq catchEventExpr(String messageRefExpr, String timeDate, String timeDuration);

        /**
         * Starts a nested builder which will add a new SubProcess to the parent builder when done
         */
        Sub<? extends Seq> sub();

        /**
         * Starts a nested builder which will add a new SubProcess (with separate execution context) to the parent builder when done
         */
        Sub<? extends Seq> sub(boolean useSeparateContext);

        /**
         * Starts a nested builder which will add a new SubProcess (with separate execution context and specified out variables) to the parent builder when done
         */
        Sub<? extends Seq> sub(boolean useSeparateContext, Set<VariableMapping> out);

        /**
         * Creates a ForkBuilder which fills a forked flow of elements. When complete, will return parent builder that will continue building flow from the point of fork.
         * <p>
         * <br/><br/>
         * Some fork examples:
         * <table>
         * <tr>
         * <th>Code</th>
         * <th>Result flow</th>
         * </tr>
         * <tr>
         * <td><pre>{@code
         * builder
         *  .task("t1")
         *  .fork().task("t2").end()
         *  .fork().task("t3").end("err")
         *  .task("t4").end()
         *    }</pre></td>
         * <td>
         * <pre>
         *               -> t2 -> [end]
         *              /
         *  [start] -> t1 -> t3 -> [end "err"]
         *              \
         *               -> t4 -> [end]
         *      </pre>
         * </td>
         * </tr>
         * <tr>
         * <td><pre>{@code
         * builder
         *  .task("t1")
         *  .fork().task("t2").joinTo("join1")
         *  .fork().task("t3").joinTo("join1")
         *  .fork().task("t4").joinTo("join1")
         *  .joinAll("join1").task("t5").end()
         *    }</pre></td>
         * <td>
         * <pre>
         *               -> t2 ->
         *              /         \
         *  [start] -> t1 -> t3 -> t5 -> [end]
         *              \         /
         *               -> t4 ->
         *      </pre>
         * </td>
         * </tr>
         * <tr>
         * <td><pre>{@code
         * builder
         *  .task("t1")
         *  .fork().task("t2").joinTo("join1")
         *  .fork().task("t3").joinTo("join1")
         *  .task("t4")
         *  .joinPoint("join1").task("t5")
         *  .end()
         *    }</pre></td>
         * <td>
         * <pre>
         *               -> t2 ->
         *              /         \
         *  [start] -> t1 -> t3 -> t5 -> [end]
         *              \         /
         *               -> t4 ->
         *      </pre>
         * </td>
         * </tr>
         * <tr>
         * <td><pre>{@code
         * builder
         *  .joinPoint("join1")
         *  .task("t1")
         *  .task("t2")
         *  .task("t3")
         *   // joins it to "t1"
         *  .fork().task("t4").joinTo("join1")
         *  .end()
         *    }</pre></td>
         * <td>
         * <pre>
         *               <- t4 <-
         *              /         \
         *  [start] -> t1 -> t3 -> t3 -> [end]
         *
         *      </pre>
         * </td>
         * </tr>
         * </table>
         */
        Fork<? extends Seq> fork();

        /**
         * Forks workflow to handle raised errors
         */
        Fork<? extends Seq> boundaryEvent();

        /**
         * Forks workflow to handle raised errors
         */
        Fork<? extends Seq> boundaryEvent(String errorRef);

        /**
         * Forks workflow to handle raised errors
         */
        Fork<? extends Seq> boundaryEvent(String errorRef, String timeDuration);

        /**
         * Marks current position as a join point. Any dangling fork tails (with the same joinName) will get joined to the next element added to the sequence.
         * The added element will become a continuation of a previous element, if you need to tie multiple forks and continue the flow from them without creating
         * another parallel flow, use {@link #joinAll(String)} method instead.
         * <br/><br/>
         * <p>
         * These two code blocks yield the same result:
         * <pre>
         * {@code
         * builder
         *   ...
         *   .fork()
         *     ...(1)
         *     .joinTo("join1")
         *   .fork()
         *     ...(2)
         *     .joinTo("join1")
         *   ...(3)
         *   .joinPoint("join1")
         *   ...
         *  }
         * </pre>
         * <pre>
         * {@code
         * builder
         *   ...
         *   .fork()
         *     ...(1)
         *     .joinTo("join1")
         *   .fork()
         *     ...(2)
         *     .joinTo("join1")
         *   .fork()
         *     ...(3)
         *     .joinTo("join1")
         *   .joinAll("join1")
         *   ...
         *  }
         * </pre>
         */
        Seq joinPoint(String joinName);

        /**
         * Marks current position as a join point. Any dangling fork tails (with the same joinName) will get joined to the next element added to the sequence.
         * Unlike with the {@link #joinPoint(String)} method the new element will be a continuation of those tails only, there will be no flow from the fork point.
         */
        Seq joinAll(String joinName);

        /**
         * Identical to joinAll but for forks that were ended with {@code Fork#loop()}
         */
        Seq tieForks();

        /**
         * Perform provided function on this object. Useful for reusing portions of builder and not break the execution chain at the same time:
         * <pre>
         * b.sub()
         *   .apply(this::doSomething);
         *   .end()
         *
         * Seq doSomething(Seq seq) {
         *   return seq
         *     .sub()
         *       ...
         *       .end();
         * }
         * </pre>
         */
        Seq apply(Function<? super Seq, ?> f);

        /**
         * Perform provided BiFunction on each element of the provided collection, similar to {@link #apply(Function)}
         */
        <E> Seq applyEach(Collection<? extends E> elements, BiFunction<? super Seq, E, ?> f);

        /**
         * Adds an EndEvent
         */
        Object endEvent();

        /**
         * Adds an EndEvent with an errorRef
         */
        Object endEvent(String errorRef);

        /**
         * Adds an EndEvent and completes current sequence
         */
        Object end();

        /**
         * Adds an EndEvent with the specified source map element and completes current sequence
         */
        Object end(Significance sig, int line, int col, String desc);

        /**
         * Adds an EndEvent with an errorRef and completes current sequence
         */
        Object end(String errorRef);

        /**
         * Adds an EndEvent with an errorRef and causeExpression and completes current sequence
         */
        Object end(String errorRef, String causeExpression);

        /**
         * Adds an EndEvent with an errorRef, causeExpression and source map and completes current sequence
         */
        Object end(String errorRef, String causeExpression, Significance sig, int line, int col, String desc);

        /**
         * Adds an TerminateEvent and completes current sequence
         */
        Object terminate();

        /**
         * Adds a source map to the last added element.
         */
        Seq sourceMap(Significance sig, int line, int col, String desc);
    }

    /**
     * Introduces type parameter for nested builders
     */
    public interface TypedSeq<T extends TypedSeq<T, P>, P> extends Seq {

        String nextStepId();

        T add(AbstractElement elem);

        default T add(Function<String, AbstractElement> elemFun) {
            return add(elemFun.apply(nextStepId()));
        }

        @Override
        default T task(ExpressionType expType, String expr) {
            return task(expType, expr, null, null);
        }

        default T task(ExpressionType expType, String expr, Set<VariableMapping> in, Set<VariableMapping> out) {
            return task(expType, expr, in, out, false);
        }

        default T task(ExpressionType expType, String expr, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables) {
            return add(new ServiceTask(nextStepId(), expType, expr, in, out, copyAllVariables));
        }

        @Override
        default T task(String expr) {
            return task(ExpressionType.SIMPLE, expr);
        }

        default T task(String expr, Set<VariableMapping> in, Set<VariableMapping> out) {
            return task(ExpressionType.SIMPLE, expr, in, out);
        }

        default T task(String expr, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables) {
            return task(ExpressionType.SIMPLE, expr, in, out, copyAllVariables);
        }

        @Override
        default T taskDelegate(String expr) {
            return task(ExpressionType.DELEGATE, expr);
        }

        @Override
        default Seq userTask(List<UserTask.Extension> extensions) {
            return add(new UserTask(nextStepId(), extensions));
        }

        @Override
        default T script(ScriptTask.Type type, String language, String content) {
            return script(type, language, content, false);
        }

        @Override
        default T script(ScriptTask.Type type, String language, String content, boolean copyAllVariables) {
            return add(new ScriptTask(nextStepId(), type, language, content, copyAllVariables));
        }

        @Override
        default T call(String calledElement) {
            return add(new CallActivity(nextStepId(), calledElement));
        }

        @Override
        default T call(String calledElement, boolean copyAllVariables) {
            return add(new CallActivity(nextStepId(), calledElement, copyAllVariables));
        }

        @Override
        default T call(String calledElement, Set<VariableMapping> in, Set<VariableMapping> out) {
            return add(new CallActivity(nextStepId(), calledElement, in, out));
        }

        @Override
        default T call(String calledElement, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables) {
            return add(new CallActivity(nextStepId(), calledElement, in, out, copyAllVariables));
        }

        @Override
        default T call(String calledElement, String calledElementExpr, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables) {
            return add(new CallActivity(nextStepId(), calledElement, calledElementExpr, in, out, copyAllVariables));
        }

        @Override
        default T eventGate() {
            return add(new EventBasedGateway(nextStepId()));
        }

        @Override
        default T parallelGate() {
            return add(new ParallelGateway(nextStepId()));
        }

        @Override
        default T inclusiveGate() {
            return add(new InclusiveGateway(nextStepId()));
        }

        @Override
        default T exclusiveGate() {
            return add(new ExclusiveGateway(nextStepId()));
        }

        @Override
        default T exclusiveGate(String defaultFlow) {
            return add(new ExclusiveGateway(nextStepId(), defaultFlow));
        }

        @Override
        T flow(String name);

        @Override
        T flowExpr(String expression);

        @Override
        T flowExpr(String name, String expression);

        @Override
        default T catchEvent() {
            return add(new IntermediateCatchEvent(nextStepId()));
        }

        @Override
        default T catchEvent(String messageRef) {
            return catchEvent(messageRef, null, null);
        }

        @Override
        default T catchEventExpr(String messageRefExpr) {
            return catchEventExpr(messageRefExpr, null, null);
        }

        @Override
        default T catchEvent(String messageRef, String timeDate, String timeDuration) {
            return add(new IntermediateCatchEvent(nextStepId(), messageRef, timeDate, timeDuration));
        }

        @Override
        default T catchEventExpr(String messageRefExpr, String timeDate, String timeDuration) {
            return add(new IntermediateCatchEvent(nextStepId(), null, messageRefExpr, timeDate, timeDuration, null));
        }

        @Override
        default T apply(Function<? super Seq, ?> f) {
            if (f != null) {
                Object s = f.apply(this);
                if (s != this) {
                    throw new IllegalStateException("Function did not return what was passed in");
                }
            }
            @SuppressWarnings("unchecked")
            T t = (T) this;
            return t;
        }

        @Override
        default <E> T applyEach(Collection<? extends E> elements, BiFunction<? super Seq, E, ?> f) {
            for (E e : elements) {
                Object s = f.apply(this, e);
                if (s != this) {
                    throw new IllegalStateException("Function did not return what was passed in");
                }
            }
            @SuppressWarnings("unchecked")
            T t = (T) this;
            return t;
        }

        /**
         * Applies provided function on this subprocess
         */
        default T applySub(Function<? super T, ?> function) {
            @SuppressWarnings("unchecked")
            Function<? super Seq, ?> f = (Function<? super Seq, ?>) function;
            return apply(f);
        }

        /**
         * Similar to {@link #applySub(Function)}, but also allows to end the sub from within the provided function
         */
        default P applyEnd(Function<? super T, ?> function) {
            @SuppressWarnings("unchecked")
            T t = (T) this;

            @SuppressWarnings("unchecked")
            P par = (P) function.apply(t);

            if (parent() != null && par != parent()) {
                throw new IllegalStateException("Function did not return the correct parent");
            }
            return par;
        }

        @Override
        Sub<T> sub();

        @Override
        Sub<T> sub(boolean useSeparateContext);

        @Override
        Sub<T> sub(boolean useSeparateContext, Set<VariableMapping> out);

        @Override
        Fork<T> fork();

        @Override
        Fork<T> boundaryEvent();

        @Override
        Fork<T> boundaryEvent(String errorRef);

        @Override
        Fork<T> boundaryEvent(String errorRef, String timeDuration);

        @Override
        T joinPoint(String joinName);

        @Override
        T joinAll(String joinName);

        @Override
        T tieForks();

        @Override
        P end();

        @Override
        P end(Significance sig, int line, int col, String desc);

        @Override
        P end(String errorRef);

        @Override
        P end(String errorRef, String causeExpression);

        @Override
        P end(String errorRef, String causeExpression, Significance sig, int line, int col, String desc);

        @Override
        P terminate();

        P parent();

        @Override
        T sourceMap(Significance sig, int line, int col, String desc);
    }

    /**
     * Nested builder which represents a sub-process
     */
    public interface Sub<P extends Seq> extends TypedSeq<Sub<P>, P> {
    }

    /**
     * Nested builder which represents a forked flow of a sequence
     */
    public interface Fork<P extends Seq> extends TypedSeq<Fork<P>, P> {
        /**
         * Completes this fork and adds a dangling tail to be joined later
         */
        P joinTo(String joinName);

        /**
         * Completes this fork as a loop, which is identical to joinTo, bu will always use parent's last element as a join point.
         * If {@code Seq#tieForks()} method is used, it works identical to {@code Seq#joinAll(String)}, but if not, an implicit {@code Seq#joinPoint(String)} is performed.
         */
        P loop();
    }

    /**
     * Top-level process builder which produces a SourceAwareProcessDefinition when complete
     */
    public interface Process extends TypedSeq<Process, SourceAwareProcessDefinition> {
    }

    private static abstract class SeqImpl<T extends TypedSeq<T, P>, P> implements TypedSeq<T, P> {

        private final List<AbstractElement> elements = new ArrayList<>();
        protected final String prefix;

        protected int stepCounter = 0;
        protected int endCounter = 0;
        protected String lastId;
        protected AbstractElement lastElement;

        protected FlowFactory flows = new FlowFactory(this);

        protected Map<String, JoinData> joins;

        protected String joinPoint;
        private boolean joinPointAll;

        SeqImpl(String prefix) {
            this.prefix = prefix;
        }

        protected List<AbstractElement> getElements() {
            return elements;
        }

        protected String nextFlowId(String from, String to) {
            if (from.startsWith(prefix)) {
                from = from.substring(prefix.length());
            }
            if (to.startsWith(prefix)) {
                to = to.substring(prefix.length());
            }

            return prefix + "flow_" + from + "_" + to;
        }

        @Override
        public String nextStepId() {
            return prefix + ++stepCounter;
        }

        protected String startId() {
            return prefix + "start";
        }

        protected String nextEndId() {
            if (endCounter++ == 0) {
                return prefix + "end";
            }
            return prefix + "end" + endCounter;
        }

        protected String loopJoin() {
            return "__loop_" + lastId;
        }

        @SuppressWarnings("unchecked")
        protected T ret() {
            return (T) this;
        }

        protected abstract P done();

        protected void addFlow(String from, String to) {
            addElement(flows.newFlow(from, to));
        }

        @Override
        public T flow(String name) {
            flows.prime(name, null);
            return ret();
        }

        @Override
        public T flowExpr(String expression) {
            flows.prime(null, expression);
            return ret();
        }

        @Override
        public T flowExpr(String name, String expression) {
            flows.prime(name, expression);
            return ret();
        }

        protected void addStart() {
            add(new StartEvent(startId()));
        }

        protected void addEnd(String errorRef, String causeExpression) {
            addEnd(errorRef, causeExpression, null, -1, -1, null);
        }

        protected void addEnd(String errorRef, String causeExpression, Significance sig, int line, int col, String desc) {
            add(new EndEvent(nextEndId(), errorRef, causeExpression));

            if (sig != null) {
                sourceMap(sig, line, col, desc);
            }

            validate();
        }

        protected void addElement(AbstractElement e) {
            elements.add(e);
        }

        @Override
        public T add(AbstractElement elem) {
            String newId = elem.getId();

            // perform joining, if needed
            String loopJoin = loopJoin();
            if (joinPoint == null && hasJoins(loopJoin)) {
                joinPoint(loopJoin);
            }
            
            // save those for the new element since doJoinPoint() might overwrite those
            String flowName = null;
            String flowExpr = null;
            if(flows.primed) {
                flowName = flows.name;
                flowExpr = flows.expression;
            }
            
            doJoinPoint(newId);

            if (lastId != null) {
                // restore flow data if needed
                if(flowName != null || flowExpr != null) {
                    flows.prime(flowName, flowExpr);
                }
                addFlow(lastId, newId);
            }
            addElement(elem);
            lastId = newId;
            lastElement = elem;

            return ret();
        }

        @Override
        public T endEvent() {
            addEnd(null, null);
            return ret();
        }

        @Override
        public T endEvent(String errorRef) {
            addEnd(errorRef, null);
            return ret();
        }

        public P end() {
            if(!(lastElement instanceof EndEvent)) {
                addEnd(null, null);
            }
            return done();
        }

        public P end(Significance sig, int line, int col, String desc) {
            addEnd(null, null, sig, line, col, desc);
            return done();
        }

        public P end(String errorRef) {
            addEnd(errorRef, null);
            return done();
        }

        public P end(String errorRef, String causeExpression) {
            addEnd(errorRef, causeExpression);
            return done();
        }

        public P end(String errorRef, String causeExpression, Significance sig, int line, int col, String desc) {
            addEnd(errorRef, causeExpression, sig, line, col, desc);
            return done();
        }

        public void addTerminate() {
            add(new TerminateEvent(nextEndId()));

            validate();
        }

        public P terminate() {
            if(!(lastElement instanceof TerminateEvent)) {
                addTerminate();
            }
            return done();
        }

        public SubImpl<T> sub() {
            return sub(false);
        }

        public SubImpl<T> sub(boolean useSeparateContext) {
            return sub(useSeparateContext, Collections.emptySet());
        }

        public SubImpl<T> sub(boolean useSeparateContext, Set<VariableMapping> out) {
            return new SubImpl<T>(this, nextStepId(), useSeparateContext, out);
        }

        public ForkImpl<T> fork() {
            return new ForkImpl<T>(this, lastId, lastElement);
        }

        public ForkImpl<T> boundaryEvent() {
            return boundaryEvent(null);
        }

        public ForkImpl<T> boundaryEvent(String errorRef) {
            return boundaryEvent(errorRef, null);
        }

        public ForkImpl<T> boundaryEvent(String errorRef, String timeDuration) {
            return new ForkImpl<T>(this, new BoundaryEvent(nextStepId(), lastId, errorRef, timeDuration));
        }

        public T joinPoint(String joinName) {
            return join(joinName, false);
        }

        public T joinAll(String joinName) {
            return join(joinName, true);
        }

        @Override
        public T tieForks() {
            return join(loopJoin(), true);
        }

        private T join(String joinName, boolean all) {
            this.joinPoint = joinName;
            this.joinPointAll = all;
            return ret();
        }

        protected void doJoin(String joinName, String id, AbstractElement element, String flowName, String flowExpr, List<Tail> additionalDanglingJoins) {
            JoinData j = getJoin(joinName);
            if(!(element instanceof EndEvent)) {
                if(additionalDanglingJoins.isEmpty()) {
                    j.danglingJoins.add(new Tail(id, flowName, flowExpr));
                }

                j.danglingJoins.addAll(additionalDanglingJoins);
            }
            flushJoins(j);
        }

        protected void doJoinPoint(String id) {
            if (joinPoint != null) {
                JoinData j = getJoin(joinPoint);
                if (j.target != null) {
                    throw new IllegalStateException("Join " + joinPoint + " target is already defined as " + j.target);
                }
                j.target = id;
                flushJoins(j);

                if (joinPointAll) {
                    lastId = null;
                    lastElement = null;
                }
                joinPoint = null;
                joinPointAll = false;
            }
        }

        private JoinData getJoin(String joinName) {
            if (joins == null) {
                joins = new HashMap<>();
            }
            JoinData j = joins.get(joinName);
            if (j == null) {
                j = new JoinData();
                joins.put(joinName, j);
            }
            return j;
        }

        private boolean hasJoins(String joinName) {
            return joins != null && joins.containsKey(joinName);
        }

        private void flushJoins(JoinData j) {
            if (j.target != null) {
                for (Tail tail: j.danglingJoins) {
                    if(tail.flowName != null || tail.flowExpression != null) {
                      flows.prime(tail.flowName, tail.flowExpression);
                    }
                    addFlow(tail.id, j.target);
                }
                j.danglingJoins.clear();
            }
        }

        protected void validate() {
            if (joins != null) {
                for (Map.Entry<String, JoinData> e : joins.entrySet()) {
                    String k = e.getKey();
                    JoinData j = e.getValue();
                    if (!j.danglingJoins.isEmpty()) {
                        throw new IllegalStateException("Not all tails were joined into " + k + " (" + j.danglingJoins + ")");
                    }
                }
            }
        }

        protected abstract void sourceMap(String id, Significance sig, int line, int col, String desc);
    }

    private static class SubImpl<P extends Seq> extends SeqImpl<Sub<P>, P> implements Sub<P> {
        private SeqImpl<?, ?> parent;
        private String id;
        private boolean useSeparateContext;
        private Set<VariableMapping> out;

        SubImpl(SeqImpl<?, ?> parent, String id, boolean useSeparateContext, Set<VariableMapping> out) {
            super(id + "_");
            this.parent = parent;
            this.id = id;
            this.useSeparateContext = useSeparateContext;
            this.out = out;
            addStart();
        }

        @Override
        protected P done() {
            parent.add(new SubProcess(id, useSeparateContext, out, getElements()));
            return parent();
        }

        @SuppressWarnings("unchecked")
        @Override
        public P parent() {
            return (P) parent;
        }

        @Override
        public Sub<P> sourceMap(Significance sig, int line, int col, String desc) {
            sourceMap(lastId, sig, line, col, desc);
            return this;
        }

        @Override
        protected void sourceMap(String id, Significance sig, int line, int col, String desc) {
            parent.sourceMap(id, sig, line, col, desc);
        }
    }

    private static class ForkImpl<P extends Seq> extends SeqImpl<Fork<P>, P> implements Fork<P> {
        private SeqImpl<?, ?> parent;

        ForkImpl(SeqImpl<?, ?> parent, String lastId, AbstractElement lastElement) {
            super(parent.prefix);
            this.parent = parent;
            this.lastId = lastId;
            this.lastElement = lastElement;
        }

        ForkImpl(SeqImpl<?, ?> parent, AbstractElement start) {
            this(parent, start.getId(), start);
            addElement(start);
        }

        @SuppressWarnings("unchecked")
        @Override
        public P parent() {
            return (P) parent;
        }

        @Override
        protected String nextFlowId(String from, String to) {
            return parent.nextFlowId(from, to);
        }

        @Override
        public String nextStepId() {
            return parent.nextStepId();
        }

        @Override
        protected String nextEndId() {
            return parent.nextEndId();
        }

        public P joinTo(String joinName) {
            P p = done();
            String flowName = flows.primed ? flows.name : null;
            String flowExpr = flows.primed ? flows.expression : null;
            parent.doJoin(joinName, lastId, lastElement, flowName, flowExpr, getActiveDanglingItems());
            return p;
        }

        @Override
        public P loop() {
            return joinTo(parent.loopJoin());
        }

        @SuppressWarnings("unchecked")
        @Override
        protected P done() {
            parent.getElements().addAll(getElements());
            return (P) parent;
        }

        private List<Tail> getActiveDanglingItems() {
            if(joinPoint == null) {
                return Collections.emptyList();
            }
            JoinData j = joins.get(joinPoint);
            if(j == null) {
                return Collections.emptyList();
            }
            return j.danglingJoins;
        }

        @Override
        public Fork<P> sourceMap(Significance sig, int line, int col, String desc) {
            sourceMap(lastId, sig, line, col, desc);
            return this;
        }

        @Override
        protected void sourceMap(String id, Significance sig, int line, int col, String desc) {
            parent.sourceMap(id, sig, line, col, desc);
        }
    }

    private static class JoinData {
        private String target;
        private List<Tail> danglingJoins = new ArrayList<>();
    }
    
    private static class Tail {
      final String id;
      final String flowName;
      final String flowExpression;
      
      public Tail(String id, String flowName, String flowExpression) {
        this.id = id;
        this.flowName = flowName;
        this.flowExpression = flowExpression;
      }
    }

    private static class FlowFactory {

        private SeqImpl<?, ?> seq;

        boolean primed = false;
        String name;
        String expression;

        public FlowFactory(SeqImpl<?, ?> seq) {
            this.seq = seq;
        }

        void prime(String name, String expression) {
            primed = true;
            this.name = name;
            this.expression = expression;
        }

        SequenceFlow newFlow(String from, String to) {
            if (primed) {
                primed = false;
                String id = name;
                if (id == null) {
                    id = seq.nextFlowId(from, to);
                }
                return new SequenceFlow(id, from, to, expression);
            }

            return new SequenceFlow(seq.nextFlowId(from, to), from, to);
        }
    }

    public static class ProcessImpl extends SeqImpl<Process, SourceAwareProcessDefinition> implements Process {

        private String id;
        private final Map<String, SourceMap> sourceMaps = new HashMap<>();

        ProcessImpl(String id) {
            super(id + "_");
            this.id = id;
            addStart();
        }

        @Override
        protected SourceAwareProcessDefinition done() {
            return new SourceAwareProcessDefinition(id, getElements(), Collections.emptyMap(), sourceMaps);
        }

        @Override
        public SourceAwareProcessDefinition parent() {
            return null;
        }

        @Override
        public Process sourceMap(Significance sig, int line, int col, String desc) {
            sourceMap(lastId, sig, line, col, desc);
            return this;
        }

        @Override
        protected void sourceMap(String id, Significance sig, int line, int col, String desc) {
            Object old = sourceMaps.put(id, new SourceMap(sig, null, line, col, desc));
            if (old != null) {
                throw new IllegalArgumentException("Duplicate source map ID: " + id);
            }
        }
    }
}
