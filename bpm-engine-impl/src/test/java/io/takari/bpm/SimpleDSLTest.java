package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.interceptors.ExecutionInterceptor;
import io.takari.bpm.api.interceptors.ExecutionInterceptorAdapter;
import io.takari.bpm.dsl.*;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class SimpleDSLTest extends AbstractEngineTest {

    @Before
    public void initDsl() {
        SimpleDSL.registerDslTask(getServiceTaskRegistry());
    }

    @Test
    public void testTrivial() throws Exception {
        Flow masterFlow = new Flow("master",
                new TaskStep("shell1"),
                new TaskStep("shell2"));

        deploy(SimpleDSL.from(masterFlow));

        // ---

        String key = "abc";
        getEngine().start(key, "master", null);

        // shuffle a bit

        getEngine().resume(key, "ok_shell1", null);
        getEngine().resume(key, "ok_shell2", null);
    }

    @Test
    public void testSimple() throws Exception {
        Flow masterFlow = new Flow("master",
                new ParallelStep(
                        new TaskStep("shell4"),
                        new TaskStep("shell5"),
                        new TaskStep("shell6")));

        deploy(SimpleDSL.from(masterFlow));

        // ---

        String key = "abc";
        getEngine().start(key, "master", null);

        // shuffle a bit

        getEngine().resume(key, "ok_shell5", null);
        getEngine().resume(key, "ok_shell4", null);
        getEngine().resume(key, "ok_shell6", null);
    }

    @Test
    public void testNested() throws Exception {
        Flow masterFlow = new Flow("master",
                new CallStep("verify"));

        Flow verifyFlow = new Flow("verify",
                new TaskStep("shell1"),
                new TaskStep("shell2"));

        deploy(SimpleDSL.from(masterFlow, verifyFlow));

        // --

        final DelegateInterceptor delegate = mock(DelegateInterceptor.class);
        ExecutionInterceptor interceptor = new ExecutionInterceptorAdapter() {
            @Override
            public void onSuspend() throws ExecutionException {
                delegate.onSuspend();
            }

            @Override
            public void onFinish(String processBusinessKey) throws ExecutionException {
                delegate.onFinish();
            }
        };
        getEngine().addInterceptor(interceptor);

        // ---

        String key = "abc";
        getEngine().start(key, "master", null);
        assertOnSuspend(delegate);

        getEngine().resume(key, "ok_shell1", null);
        assertOnSuspend(delegate);

        getEngine().resume(key, "ok_shell2", null);
        dumpActivations();
        assertOnFinish(delegate);
    }

    @Test
    public void testDeeplyNested() throws Exception {
        Flow masterFlow = new Flow("master",
                new CallStep("verify"));

        Flow setupFlow = new Flow("setup",
                new TaskStep("shell1"));

        Flow verifyFlow = new Flow("verify",
                new CallStep("setup"),
                new TaskStep("shell7"));

        deploy(SimpleDSL.from(masterFlow, setupFlow, verifyFlow));

        // --

        final DelegateInterceptor delegate = mock(DelegateInterceptor.class);
        ExecutionInterceptor interceptor = new ExecutionInterceptorAdapter() {
            @Override
            public void onSuspend() throws ExecutionException {
                delegate.onSuspend();
            }

            @Override
            public void onFinish(String processBusinessKey) throws ExecutionException {
                delegate.onFinish();
            }
        };
        getEngine().addInterceptor(interceptor);

        // ---

        String key = "abc";
        getEngine().start(key, "master", null);
        assertOnSuspend(delegate);

        getEngine().resume(key, "ok_shell1", null);
        assertOnSuspend(delegate);

        // last one

        getEngine().resume(key, "ok_shell7", null);
        assertOnFinish(delegate);
    }

    private static void assertOnSuspend(DelegateInterceptor interceptor) throws Exception {
        verify(interceptor, times(1)).onSuspend();
        verifyNoMoreInteractions(interceptor);
        reset(interceptor);
    }

    private static void assertOnFinish(DelegateInterceptor interceptor) throws Exception {
        verify(interceptor, times(1)).onFinish();
        verifyNoMoreInteractions(interceptor);
        reset(interceptor);
    }

    public interface DelegateInterceptor {

        void onSuspend();

        void onFinish();
    }
}
