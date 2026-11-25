package io.takari.bpm.utils;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs the delegate in the specified executor and waits for its completion. If the delegate doesn't finish
 * before the first of the timeouts, the delegate is cancelled (interrupted) and the timeout's payload is
 * returned. A timed out delegate may still be running; its results must be discarded (see {@link #isTimedOut()}).
 * <p>
 * Exceptions thrown by the delegate are rethrown as is.
 */
public class TimeoutCallable<T> implements Callable<T> {

    private final ExecutorService executor;
    private final List<Timeout<T>> timeouts;
    private final Callable<T> delegate;

    private volatile boolean timedOut;

    public TimeoutCallable(ExecutorService executor, List<Timeout<T>> timeouts, Callable<T> delegate) {
        if (timeouts == null || timeouts.isEmpty()) {
            throw new IllegalArgumentException("At least one timeout is required");
        }

        this.executor = executor;
        this.timeouts = timeouts;
        this.delegate = delegate;
    }

    @Override
    public T call() throws Exception {
        Timeout<T> first = timeouts.stream()
                .min(Comparator.comparingLong(Timeout::getDuration))
                .orElseThrow();

        Future<T> f = executor.submit(delegate);
        try {
            return f.get(Math.max(0, first.getDuration()), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw e;
        } catch (TimeoutException e) {
            timedOut = true;
            f.cancel(true);
            return first.getPayload();
        } catch (InterruptedException e) {
            f.cancel(true);
            throw e;
        }
    }

    /**
     * @return {@code true} if the delegate didn't finish in time.
     */
    public boolean isTimedOut() {
        return timedOut;
    }
}
