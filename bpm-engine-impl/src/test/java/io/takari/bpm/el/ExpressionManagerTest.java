package io.takari.bpm.el;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.takari.bpm.api.ExecutionContext;
import io.takari.bpm.api.ExecutionContextFactory;
import io.takari.bpm.api.JavaDelegate;
import io.takari.bpm.api.Variables;
import io.takari.bpm.context.DefaultExecutionContextFactory;
import io.takari.bpm.context.ExecutionContextImpl;
import io.takari.bpm.task.KeyAwareServiceTaskRegistry;
import io.takari.bpm.task.ServiceTaskRegistry;
import org.junit.Test;

import java.security.spec.ECField;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ExpressionManagerTest {

    /**
     {
       "x" : {
         "y" : {
           "zz" : "11",
           "z" : "${x.y.zz}"
         }
       }
     }
     */
    @Test
    public void test1() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Object> m1 = new LinkedHashMap<>();
        m1.put("zz", "11");
        m1.put("z", "${x.y.zz}");
        m.put("x", Collections.singletonMap("y", m1));

        Map<String, Object> expected = new LinkedHashMap<>();
        Map<String, Object>  e1 = new LinkedHashMap<>();
        e1.put("z", "11");
        e1.put("zz", "11");
        expected.put("x", Collections.singletonMap("y", e1));

        // ---
        assertInterpolate(expected, m);
    }

    @Test
    public void test2() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", Collections.singletonMap("y", Collections.singletonMap("z", Arrays.asList("a", "${x}"))));

        Map<String, Object> expected = Collections.singletonMap("x",
                Collections.singletonMap("y",
                        Collections.singletonMap("z", Arrays.asList("a", 123))));

        Map<String, Object> currentVariables = Collections.singletonMap("x", 123);

        // ---
        assertInterpolate(expected, currentVariables, m);
    }



    /**
     - set:
     x: 123
     y: ${x}
     */
    @Test
    public void testDependentInterpolate() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", 123);
        m.put("y", "${x}");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("x", 123);
        expected.put("y", 123);

        // ---
        assertInterpolate(expected, m);
    }

    /**
     * - set:
     *   x: 123
     *
     * - set:
     *   x:
     *     y: "${x}"
     */
    @Test
    public void testDependentOuterInterpolate() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", Collections.singletonMap("y", "${x}"));

        Map<String, Object> expected = Collections.singletonMap("x", Collections.singletonMap("y", 123));

        Map<String, Object> currentVariables = Collections.singletonMap("x", 123);

        // ---
        assertInterpolate(expected, currentVariables, m);
    }

    @Test
    public void testInterpolation() throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();

        m.put("primaryServers", new String[]{"127.0.0.1"});

        Map<String, Object> primary = new LinkedHashMap<>();
        primary.put("servers", "${primaryServers}");

        Map<String, Object> active = new LinkedHashMap<>();
        active.put("servers", "${landscape.primary.servers}");

        Map<String, Object> landscape = new LinkedHashMap<>();
        landscape.put("primary", primary);
        landscape.put("active", active);

        m.put("landscape", landscape);

        // ---

        ExpressionManager em = new DefaultExpressionManager(mock(ServiceTaskRegistry.class));
        DefaultExecutionContextFactory ctxFactory = new DefaultExecutionContextFactory(em);

        ExecutionContext ctx = new ExecutionContextImpl(ctxFactory, em, new Variables());
        m = (Map<String, Object>) Interpolator.interpolate(ctxFactory, em, ctx, m);

        // ---
        assertEquals(2, m.size());

        Object[] as = (Object[]) ((Map<String, Object>) ((Map<String, Object>) m.get("landscape")).get("active")).get("servers");
        assertEquals(1, as.length);
        assertEquals("127.0.0.1", as[0]);
    }

    @Test
    public void testEvalInt() throws Exception {
        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        String exp = "${1+1}";
        Integer result = em.eval(mock(ExecutionContext.class), exp, Integer.class);

        assertEquals((Integer)2, result);
    }

    @Test
    public void testStringConcat() throws Exception {
        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        String exp = "${\"1\" += \"1\"}";
        String result = em.eval(mock(ExecutionContext.class), exp, String.class);

        assertEquals("11", result);
    }

    @Test
    public void testGetVariableFromExecutionFull() throws Exception {
        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        ExecutionContext execution = mock(ExecutionContext.class);
        when(execution.getVariable(eq("var1"))).thenReturn("var1Result");
        when(execution.hasVariable(eq("var1"))).thenReturn(true);

        String exp = "${execution.getVariable('var1')}";
        String result = em.eval(execution, exp, String.class);

        assertEquals("var1Result", result);
    }

    @Test
    public void testGetVariableFromExecutionShort() throws Exception {
        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        ExecutionContext execution = mock(ExecutionContext.class);
        when(execution.getVariable(eq("var1"))).thenReturn("var1Result");
        when(execution.hasVariable(eq("var1"))).thenReturn(true);

        String exp = "${var1}";
        String result = em.eval(execution, exp, String.class);

        assertEquals("var1Result", result);
    }

    @Test
    public void testTaskExecutionFull() throws Exception {
        TestTask task = spy(new TestTask());

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        when(taskRegistry.getByKey(eq("task1"))).thenReturn(task);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        String exp = "${task1.execute(execution)}";
        em.eval(mock(ExecutionContext.class), exp, Object.class);

        verify(task, times(1)).execute(any());
    }

    @Test
    public void testTaskExecutionShort() throws Exception {
        ExecutionContext context = mock(ExecutionContext.class);

        TestTask task = spy(new TestTask());

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        when(taskRegistry.getByKey(eq("task1"))).thenReturn(task);

        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        String exp = "${task1}";
        JavaDelegate result = em.eval(context, exp, JavaDelegate.class);

        assertNotNull(result);
        result.execute(context);

        verify(task, times(1)).execute(any());
    }

    @Test
    public void testLambda() throws Exception {
        ExecutionContext context = mock(ExecutionContext.class);

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        String exp = "${(x -> x + 1)(10)}";
        Integer result = em.eval(context, exp, Integer.class);

        assertEquals((Integer)11, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testList() throws Exception {
        ExecutionContext context = mock(ExecutionContext.class);

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        String exp = "${[1, 2, 3]}";
        List<Long> result = em.eval(context, exp, List.class);

        assertEquals(ImmutableList.of(1L, 2L, 3L), result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSet() throws Exception {
        ExecutionContext context = mock(ExecutionContext.class);

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        String exp = "${{1, 2, 3}}";
        Set<Long> result = em.eval(context, exp, Set.class);

        assertEquals(ImmutableSet.of(1L, 2L, 3L), result);
    }

    @Test
    public void testStatic() throws Exception {
        ExecutionContext context = mock(ExecutionContext.class);

        KeyAwareServiceTaskRegistry taskRegistry = mock(KeyAwareServiceTaskRegistry.class);
        ExpressionManager em = new DefaultExpressionManager(taskRegistry);

        String exp = "${Boolean.TRUE}";
        Boolean result = em.eval(context, exp, Boolean.class);

        assertEquals(true, result);
    }

    private void assertInterpolate(Map<String, Object> expected, Map<String, Object> i) throws Exception {
        assertInterpolate(expected, Collections.emptyMap(), i);
    }

    @SuppressWarnings("unchecked")
    private void assertInterpolate(Map<String, Object> expected, Map<String, Object> vars, Map<String, Object> i) throws Exception {
        System.out.println(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(i));

        ExpressionManager em = new DefaultExpressionManager(mock(ServiceTaskRegistry.class));
        DefaultExecutionContextFactory ctxFactory = new DefaultExecutionContextFactory(em);

        ExecutionContext ctx = new ExecutionContextImpl(ctxFactory, em, new Variables(vars));
        Map<String, Object> result = (Map<String, Object>) Interpolator.interpolate(ctxFactory, em, ctx, i);
        assertEquals(expected, result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> convert(Map<String, Object> in) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> e : in.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map) {
                v = convert((Map<String, Object>) v);
            }
            result.put(e.getKey(), v);
        }
        return result;
    }

    class TestTask implements JavaDelegate {

        @Override
        public void execute(ExecutionContext ctx) throws Exception {
            // do nothing
        }
    }
}
