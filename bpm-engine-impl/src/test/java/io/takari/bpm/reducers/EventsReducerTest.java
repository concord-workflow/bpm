package io.takari.bpm.reducers;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventsReducerTest {

    @Test
    public void testParseIso8601LocalDateTime() {
        Date actual = EventsReducer.parseIso8601("2011-03-11T12:13:14");
        Date expected = Date.from(LocalDateTime.of(2011, 3, 11, 12, 13, 14)
                .atZone(ZoneId.systemDefault())
                .toInstant());
        assertEquals(expected, actual);
    }

    @Test
    public void testParseIso8601UtcInstant() {
        Date actual = EventsReducer.parseIso8601("2011-03-11T12:13:14Z");
        Date expected = Date.from(Instant.parse("2011-03-11T12:13:14Z"));
        assertEquals(expected, actual);
    }

    @Test
    public void testParseIso8601WithOffset() {
        Date actual = EventsReducer.parseIso8601("2011-03-11T14:13:14+02:00");
        Date expected = Date.from(Instant.parse("2011-03-11T12:13:14Z"));
        assertEquals(expected, actual);
    }

    @Test
    public void testParseIso8601DateOnly() {
        Date actual = EventsReducer.parseIso8601("2011-03-11");
        Date expected = Date.from(LocalDate.of(2011, 3, 11)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
        assertEquals(expected, actual);
    }
}
