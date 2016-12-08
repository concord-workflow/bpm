package io.takari.bpm.state;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ActivationsTest {

    @Test
    public void test() throws Exception {
        UUID scopeA = UUID.randomUUID();
        UUID scopeB = UUID.randomUUID();
        UUID scopeC = UUID.randomUUID();

        Activations a = new Activations()
                .inc(scopeA, "a", 1)
                .inc(scopeA, "b", 3)
                .inc(scopeB, "a", 123);

        assertEquals(1, a.count(scopeA, "a"));
        assertEquals(3, a.count(scopeA, "b"));
        assertEquals(123, a.count(scopeB, "a"));
        assertEquals(0, a.count(scopeC, "z"));
    }
}
