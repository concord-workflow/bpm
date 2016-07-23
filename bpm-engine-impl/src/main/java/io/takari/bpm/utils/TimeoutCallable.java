package io.takari.bpm.utils;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.takari.bpm.api.BpmnError;

public class TimeoutCallable<T> implements Callable<T> {

    private static final long MIN_TIMER_FREQUENCY = 1000;

    private final ExecutorService executor;
    private final List<Timeout<T>> timeouts;
    private final Callable<T> delegate;

    public TimeoutCallable(ExecutorService executor, List<Timeout<T>> timeouts, Callable<T> delegate) {
        this.executor = executor;
        this.timeouts = timeouts;
        this.delegate = delegate;
    }

    @Override
    public T call() throws Exception {
        long now = System.currentTimeMillis();
        Future<T> f = executor.submit(delegate);

        // poll until the task completion
        while (true) {
            try {
                // task is finished, proceed as usual
                return f.get(MIN_TIMER_FREQUENCY, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof BpmnError) {
                    throw (BpmnError) cause;
                }
            } catch (TimeoutException e) {
                long now2 = System.currentTimeMillis();
                for (Timeout<T> t : timeouts) {
                    long dt = now2 - now;
                    if (t.getDuration() <= dt) {
                        f.cancel(true);
                        return t.getPayload();
                    }
                }
            }
        }
    }
}
