package io.takari.bpm.state;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventsTest {

    @Test
    public void testEmpty() throws Exception {
        UUID scopeA = UUID.randomUUID();
        UUID scopeB = UUID.randomUUID();
        UUID scopeC = UUID.randomUUID();

        UUID event1 = UUID.randomUUID();

        Scopes scopes = new Scopes()
                .push(scopeA, false)
                .push(scopeB, false)
                .push(scopeC, false);

        Events events = new Events()
                .addEvent(scopeB, event1, "test");

        assertTrue(events.isEmpty(scopes, scopeC));
        assertFalse(events.isEmpty(scopes, scopeB));
        assertFalse(events.isEmpty(scopes, scopeA));
    }

    @Test
    public void testSiblings() throws Exception {
        UUID scopeA = UUID.randomUUID();
        UUID scopeB = UUID.randomUUID();
        UUID scopeC = UUID.randomUUID();
        UUID scopeD = UUID.randomUUID();

        UUID event1 = UUID.randomUUID();

        Scopes scopes = new Scopes()
                .push(scopeA, false)
                .push(scopeB, false)
                .push(scopeC, false)
                .pop()
                .push(scopeD, false);

        Events events = new Events()
                .addEvent(scopeD, event1, "test");

        assertFalse(events.isEmpty(scopes, scopeB));
        assertTrue(events.isEmpty(scopes, scopeC));
    }
}
