package io.takari.bpm;

import io.takari.bpm.api.BpmnError;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class SubProcessTest extends AbstractEngineTest {

    /**
     * start --> sub                            t2 --> end
     *              \                          /
     *               substart --> t1 --> subend
     */
    @Test
    public void testSimple() throws Exception {
        JavaDelegate t1 = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("t1", t1);

        JavaDelegate t2 = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("t2", t2);

        // --

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", Arrays.asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "t1"),
                        new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                        new SequenceFlow("f3", "t1", "subend"),
                        new EndEvent("subend")
                )),
                new SequenceFlow("f4", "sub", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f5", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "sub",
                "substart",
                "f2",
                "t1",
                "f3",
                "subend",
                "f4",
                "t2",
                "f5",
                "end");
        assertNoMoreActivations();

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> sub                            t2 ------------------> end
     *              \                          /                   /
     *               substart --> t1 --> subend --> error --> t3 --
     */
    @Test
    public void testBoundaryError() throws Exception {
        final String errorRef = "test#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws ExecutionException {
                throw new BpmnError(errorRef);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        JavaDelegate t2 = mock(JavaDelegate.class);
        getServiceTaskRegistry().register("t2", t2);

        JavaDelegate t3 = spy(new JavaDelegate() {

            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                BpmnError e = (BpmnError) ctx.getVariable(ExecutionContext.LAST_ERROR_KEY);
                assertNotNull(e);
                assertEquals(errorRef, e.getErrorRef());
            }
        });
        getServiceTaskRegistry().register("t3", t3);

        // ---

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", Arrays.asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "t1"),
                        new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                        new SequenceFlow("f3", "t1", "subend"),
                        new EndEvent("subend")
                )),
                new BoundaryEvent("be1", "sub", errorRef),
                new SequenceFlow("f4", "be1", "t3"),
                new ServiceTask("t3", ExpressionType.DELEGATE, "${t3}"),
                new SequenceFlow("f5", "t3", "end"),

                new SequenceFlow("f6", "sub", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f7", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "sub",
                "substart",
                "f2",
                "t1",
                "be1",
                "f4",
                "t3",
                "f5",
                "end");
        assertNoMoreActivations();

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verifyZeroInteractions(t2);
        verify(t3, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> sub                                -----------> end
     *              \                              /            /
     *               substart --> t1 --> suberrend --> error -->
     */
    @Test
    public void testErrorEnd() throws Exception {
        final String errorRef = "test#" + System.currentTimeMillis();

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", Arrays.asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "subend"),
                        new EndEvent("subend", errorRef)
                )),
                new BoundaryEvent("be1", "sub", errorRef),
                new SequenceFlow("f4", "be1", "end"),

                new SequenceFlow("f6", "sub", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        assertActivations(key, processId,
                "start",
                "f1",
                "sub",
                "substart",
                "f2",
                "subend",
                "be1",
                "f4",
                "end");
        assertNoMoreActivations();
    }

    /**
     * start --> sub                            t2 --> end
     *              \                          /
     *               substart --> t1 --> subend
     */
    @Test
    public void testVariablePassing() throws Exception {
        final String k = "k" + System.currentTimeMillis();
        final String v = "v" + System.currentTimeMillis();
        final String shadowV = "s" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                ctx.setVariable(k, v);
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        JavaDelegate t2 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                Object v2 = ctx.getVariable(k);
                assertEquals(v, v2);
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // --

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", Arrays.asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "t1"),
                        new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                        new SequenceFlow("f3", "t1", "subend"),
                        new EndEvent("subend")
                )),
                new SequenceFlow("f4", "sub", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f5", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        Map<String, Object> m = new HashMap<>();
        m.put(k, shadowV);
        getEngine().start(key, processId, m);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }


    /**
     * start --> sub                            t2 --> end
     *              \                          /
     *               substart --> t1 --> subend
     */
    @Test
    public void testSeparateContext() throws Exception {
        String varKey = "var#" + System.currentTimeMillis();
        String varVal = "val#" + System.currentTimeMillis();

        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                assertEquals(varVal, ctx.getVariable(varKey));
                ctx.setVariable(varKey, "something#" + System.currentTimeMillis());
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        JavaDelegate t2 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                assertEquals(varVal, ctx.getVariable(varKey));
            }
        });
        getServiceTaskRegistry().register("t2", t2);

        // --

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", true, Arrays.asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "t1"),
                        new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                        new SequenceFlow("f3", "t1", "subend"),
                        new EndEvent("subend")
                )),
                new SequenceFlow("f4", "sub", "t2"),
                new ServiceTask("t2", ExpressionType.DELEGATE, "${t2}"),
                new SequenceFlow("f5", "t2", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        Map<String, Object> args = Collections.singletonMap(varKey, varVal);
        getEngine().start(key, processId, args);

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
        verify(t2, times(1)).execute(any(ExecutionContext.class));
    }

    /**
     * start --> sub                            --> end
     *              \                          /
     *               substart --> t1 --> subend
     */
    @Test
    public void testSeparateContextErrorPropagation() throws Exception {
        JavaDelegate t1 = spy(new JavaDelegate() {
            @Override
            public void execute(ExecutionContext ctx) throws Exception {
                throw new BpmnError("kaboom!");
            }
        });
        getServiceTaskRegistry().register("t1", t1);

        // --

        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "sub"),
                new SubProcess("sub", true, Arrays.asList(
                        new StartEvent("substart"),
                        new SequenceFlow("f2", "substart", "t1"),
                        new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
                        new SequenceFlow("f3", "t1", "subend"),
                        new EndEvent("subend")
                )),
                new SequenceFlow("f4", "sub", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        try {
            getEngine().start(key, processId, null);
            fail("should throw an exception");
        } catch (ExecutionException e) {
        }

        // ---

        verify(t1, times(1)).execute(any(ExecutionContext.class));
    }
    
    /**
     * start --> sub                     t1 --> end
     *              \                   /
     *               substart --> subend
     */
    @Test
    public void testSeparateContextOutVariables() throws Exception {
      final String beforeSub = "beforeSub" + System.currentTimeMillis();
      final String afterSub = "afterSub" + System.currentTimeMillis();
      final Object v = "v" + System.currentTimeMillis();

      Set<VariableMapping> outs = new HashSet<>();
      outs.add(VariableMapping.eval("${" + beforeSub + "}", afterSub));

      String processId = "test";
      deploy(new ProcessDefinition(processId, Arrays.asList(
              new StartEvent("start"),
              new SequenceFlow("f1", "start", "sub"),
              new SubProcess("sub", true, outs, Arrays.asList(
                      new StartEvent("substart"),
                      new SequenceFlow("f2", "substart", "subend"),
                      new EndEvent("subend")
              )),
              new SequenceFlow("f3", "sub", "t1"),
              new ServiceTask("t1", ExpressionType.DELEGATE, "${t1}"),
              new SequenceFlow("f4", "t1", "end"),
              new EndEvent("end")
     )));

      JavaDelegate t1Task = spy(new JavaDelegate() {
          @Override
          public void execute(ExecutionContext ctx) throws Exception {
              Object o = ctx.getVariable(afterSub);
              assertEquals(v, o);
          }
      });
      getServiceTaskRegistry().register("t1", t1Task);

      // ---

      String key = UUID.randomUUID().toString();
      Map<String, Object> input = new HashMap<>();
      input.put(beforeSub, v);
      getEngine().start(key, processId, input);

      // ---

      assertActivations(key, processId,
              "start",
              "f1",
              "sub");

      assertActivations(key, processId,
              "substart",
              "f2",
              "subend");

      assertActivations(key, processId,
              "f3",
              "t1",
              "f4",
              "end");

      assertNoMoreActivations();

      // ---

      verify(t1Task, times(1)).execute(any(ExecutionContext.class));
    }
}
