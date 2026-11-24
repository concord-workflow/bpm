package io.takari.bpm.lock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StripedLockManagerImplTest {

    @Test
    public void testInvalidConcurrencyLevel() {
        assertThrows(IllegalArgumentException.class, () -> new StripedLockManagerImpl(0));
        assertThrows(IllegalArgumentException.class, () -> new StripedLockManagerImpl(-4));
    }

    @Test
    public void testReentrancyAndMutualExclusion() throws Exception {
        StripedLockManagerImpl manager = new StripedLockManagerImpl(3);
        manager.lock("key-1");
        try {
            manager.lock("key-1");
            manager.unlock("key-1");
        } finally {
            manager.unlock("key-1");
        }

        int threads = 8;
        int iterations = 200;
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(threads);

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < iterations; j++) {
                            manager.lock("shared-key");
                            try {
                                int v = counter.get();
                                counter.set(v + 1);
                            } finally {
                                manager.unlock("shared-key");
                            }
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertTrue(done.await(10, TimeUnit.SECONDS));
        }

        assertEquals(threads * iterations, counter.get());
    }
}
