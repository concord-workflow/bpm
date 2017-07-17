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

        Scopes scopesA = new Scopes().push(scopeA, false);
        Scopes scopesB = new Scopes().push(scopeB, false);
        Scopes scopesC = scopesB.push(scopeC, false);
        
        Activations a = new Activations()
                .incExpectation(scopesA, scopeA, "a", 1).inc(scopesA, scopeA, "a", 1)
                .incExpectation(scopesA, scopeA, "b", 3).inc(scopesA, scopeA, "b", 3)
                .incExpectation(scopesB, scopeB, "a", 123).inc(scopesB, scopeB, "a", 50);

        assertEquals(1, a.getActivation(scopesA, scopeA, "a").getExpected());
        assertEquals(1, a.getActivation(scopesA, scopeA, "a").getReceived());
        
        assertEquals(3, a.getActivation(scopesA, scopeA, "b").getExpected());
        assertEquals(3, a.getActivation(scopesA, scopeA, "b").getReceived());
        
        assertEquals(123, a.getActivation(scopesB, scopeB, "a").getExpected());
        assertEquals(50, a.getActivation(scopesC, scopeB, "a").getReceived());
        
        assertEquals(0, a.getActivation(scopesC, scopeC, "z").getExpected());
        assertEquals(0, a.getActivation(scopesC, scopeC, "z").getReceived());
    }
}
