package io.takari.bpm;

import io.takari.bpm.api.Event;
import io.takari.bpm.api.EventService;
import io.takari.bpm.model.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventServiceTest extends AbstractEngineTest {

    /**
     * start --> gw --> ev --> end
     */
    @Test
    public void testSingleEvent() throws Exception {
        String processId = "test";
        deploy(new ProcessDefinition(processId, Arrays.asList(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "gw"),
                new EventBasedGateway("gw"),
                new SequenceFlow("f2", "gw", "ev"),
                new IntermediateCatchEvent("ev", "ev"),
                new SequenceFlow("f3", "ev", "end"),
                new EndEvent("end")
        )));

        // ---

        String key = UUID.randomUUID().toString();
        getEngine().start(key, processId, null);

        // ---

        EventService es = getEngine().getEventService();

        Collection<Event> evs = es.getEvents(key);
        assertNotNull(evs);
        assertEquals(1, evs.size());

        Event ev = evs.iterator().next();
        assertEquals(key, ev.getProcessBusinessKey());
        assertEquals("ev", ev.getName());

        // ---

        getEngine().resume(ev.getId(), null);
    }
}
