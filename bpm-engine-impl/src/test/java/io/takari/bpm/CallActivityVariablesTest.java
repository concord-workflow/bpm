package io.takari.bpm;

import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Variables of call activities and subprocesses with a separate context survive suspension.
 */
public class CallActivityVariablesTest extends AbstractEngineTest {

    private final List<String> seen = new CopyOnWriteArrayList<>();

    private void registerPeek(String name, String... keys) {
        getServiceTaskRegistry().register(name, (JavaDelegate) ctx -> {
            StringBuilder b = new StringBuilder();
            for (String k : keys) {
                if (b.length() > 0) {
                    b.append(",");
                }
                b.append(k).append("=").append(ctx.getVariable(k));
            }
            seen.add(b.toString());
        });
    }

    /**
     * The child sees only the mapped variables, before and after it is suspended.
     * <pre>
     * parent: start --> call --> peek --> end
     * child:  start --> peek --> ev --> peek --> end
     * </pre>
     */
    @Test
    public void testIsolationAcrossSuspend() throws Exception {
        registerPeek("peek", "secret", "x", "y");

        deploy(new ProcessDefinition("parent", Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "call"),
                new CallActivity("call", "child",
                        new HashSet<>(Set.of(VariableMapping.copy("x", "x"))),
                        new HashSet<>(Set.of(VariableMapping.copy("y", "y")))),
                new SequenceFlow("f2", "call", "p0"),
                new ServiceTask("p0", ExpressionType.DELEGATE, "${peek}"),
                new SequenceFlow("f3", "p0", "end"),
                new EndEvent("end"))));

        deploy(new ProcessDefinition("child", Arrays.asList(
                new StartEvent("cstart"),
                new SequenceFlow("c1", "cstart", "p1"),
                new ServiceTask("p1", ExpressionType.DELEGATE, "${peek}"),
                new SequenceFlow("c2", "p1", "cev"),
                new IntermediateCatchEvent("cev", "cev"),
                new SequenceFlow("c3", "cev", "p2"),
                new ServiceTask("p2", ExpressionType.DELEGATE, "${peek}"),
                new SequenceFlow("c4", "p2", "cend"),
                new EndEvent("cend"))));

        String key = UUID.randomUUID().toString();
        Map<String, Object> args = new HashMap<>();
        args.put("secret", "S");
        args.put("x", "X");
        getEngine().start(key, "parent", args);

        // the resume variables go to the child
        getEngine().resume(key, "cev", Map.of("y", "Y"));

        assertEquals(List.of(
                "secret=null,x=X,y=null",
                "secret=null,x=X,y=Y",
                "secret=S,x=X,y=Y"), seen);
    }

    /**
     * A call activity in a parallel branch doesn't overwrite the variables set by the other branch.
     * <pre>
     * start --> gw1 --> call ----------------> gw2 --> peek --> end
     *              \                          /
     *               --> setB --> bev ---------
     * child: start --> cev --> end
     * </pre>
     */
    @Test
    public void testCallActivityInParallelBranch() throws Exception {
        getServiceTaskRegistry().register("setB", (JavaDelegate) ctx -> ctx.setVariable("b", "B"));
        registerPeek("peek", "b");

        deploy(new ProcessDefinition("parent", Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw1"),
                new ParallelGateway("gw1"),
                new SequenceFlow("f2", "gw1", "call"),
                new CallActivity("call", "child", true),
                new SequenceFlow("f3", "call", "gw2"),
                new SequenceFlow("f4", "gw1", "setB"),
                new ServiceTask("setB", ExpressionType.DELEGATE, "${setB}"),
                new SequenceFlow("f5", "setB", "bev"),
                new IntermediateCatchEvent("bev", "bev"),
                new SequenceFlow("f6", "bev", "gw2"),
                new ParallelGateway("gw2"),
                new SequenceFlow("f7", "gw2", "peek"),
                new ServiceTask("peek", ExpressionType.DELEGATE, "${peek}"),
                new SequenceFlow("f8", "peek", "end"),
                new EndEvent("end"))));

        deploy(new ProcessDefinition("child", Arrays.asList(
                new StartEvent("cstart"),
                new SequenceFlow("c1", "cstart", "cev"),
                new IntermediateCatchEvent("cev", "cev"),
                new SequenceFlow("c2", "cev", "cend"),
                new EndEvent("cend"))));

        for (String[] order : new String[][]{{"bev", "cev"}, {"cev", "bev"}}) {
            seen.clear();

            String key = UUID.randomUUID().toString();
            getEngine().start(key, "parent", null);
            for (String ev : order) {
                getEngine().resume(key, ev, null);
            }

            assertEquals(List.of("b=B"), seen, "order: " + Arrays.toString(order));
        }
    }

    /**
     * A subprocess with a separate context keeps its variables while suspended.
     * <pre>
     * start --> sub --> peek --> end
     * sub: start --> set --> ev --> peek --> end
     * </pre>
     */
    @Test
    public void testSubprocessWithSeparateContext() throws Exception {
        getServiceTaskRegistry().register("set", (JavaDelegate) ctx -> ctx.setVariable("inner", "I"));
        registerPeek("peek", "outer", "inner", "result");

        deploy(new ProcessDefinition("test", Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", true, new HashSet<>(Set.of(VariableMapping.copy("inner", "result"))), Arrays.asList(
                        new StartEvent("sstart"),
                        new SequenceFlow("s1", "sstart", "set"),
                        new ServiceTask("set", ExpressionType.DELEGATE, "${set}"),
                        new SequenceFlow("s2", "set", "ev"),
                        new IntermediateCatchEvent("ev", "ev"),
                        new SequenceFlow("s3", "ev", "p1"),
                        new ServiceTask("p1", ExpressionType.DELEGATE, "${peek}"),
                        new SequenceFlow("s4", "p1", "send"),
                        new EndEvent("send"))),
                new SequenceFlow("f2", "sub", "p2"),
                new ServiceTask("p2", ExpressionType.DELEGATE, "${peek}"),
                new SequenceFlow("f3", "p2", "end"),
                new EndEvent("end"))));

        String key = UUID.randomUUID().toString();
        getEngine().start(key, "test", Map.of("outer", "O"));
        getEngine().resume(key, "ev", null);

        assertEquals(List.of(
                "outer=O,inner=I,result=null",
                "outer=O,inner=null,result=I"), seen);
    }
}
