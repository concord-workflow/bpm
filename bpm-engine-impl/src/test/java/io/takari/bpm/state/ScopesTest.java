package io.takari.bpm.state;

import io.takari.bpm.state.Scopes.Scope;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ScopesTest {

    @Test
    public void testTraverse() throws Exception {
        UUID scopeA = UUID.randomUUID();
        UUID scopeB = UUID.randomUUID();
        UUID scopeC = UUID.randomUUID();
        UUID scopeD = UUID.randomUUID();

        Scopes scopes = new Scopes()
                .push(scopeA, false)
                .push(scopeB, false)
                .pop()
                .push(scopeC, false)
                .push(scopeD, false);

        List<Scope> l1 = scopes.traverse(scopeD);
        assertEquals(3, l1.size());
        assertEquals(scopeD, l1.get(0).getId());
        assertEquals(scopeC, l1.get(1).getId());
        assertEquals(scopeA, l1.get(2).getId());

        List<Scope> l2 = scopes.traverse(scopeB);
        assertEquals(2, l2.size());
        assertEquals(scopeB, l2.get(0).getId());
        assertEquals(scopeA, l2.get(1).getId());
    }
}
