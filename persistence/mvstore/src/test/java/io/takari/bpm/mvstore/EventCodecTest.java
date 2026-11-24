package io.takari.bpm.mvstore;

import io.takari.bpm.event.Event;
import io.takari.bpm.mvstore.EventCodec.EventMetadata;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventCodecTest {

    private final Serializer serializer = new JavaSerializer();

    @Test
    void roundTripsFullEvent() {
        UUID id = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        UUID scopeId = UUID.randomUUID();
        Date expiredAt = new Date(1_700_000_000_123L);
        Map<String, String> payload = Map.of("k", "v");

        Event original = new Event(id, executionId, "proc-def-1", scopeId, "ev\0name-🔥", "bk-1", true, expiredAt, payload);
        byte[] encoded = EventCodec.encode(original, serializer);
        Event decoded = EventCodec.decode(encoded, serializer);

        assertEquals(original, decoded);
        assertEquals(original.getDefinitionId(), decoded.getDefinitionId());
        assertEquals(original.getPayload(), decoded.getPayload());
    }

    @Test
    void roundTripsMinimalEventWithNullOptionalFields() {
        UUID id = UUID.randomUUID();
        Event original = new Event(id, null, null, null, null, null, false, null, null);

        byte[] encoded = EventCodec.encode(original, serializer);
        Event decoded = EventCodec.decode(encoded, serializer);

        assertEquals(original, decoded);
        assertNull(decoded.getDefinitionId());
        assertNull(decoded.getPayload());
    }

    @Test
    void roundTripsPreEpochTimestamp() {
        UUID id = UUID.randomUUID();
        Event original = new Event(id, null, "def", null, "ev", "bk", false, new Date(-86_400_000L), null);

        byte[] encoded = EventCodec.encode(original, serializer);
        Event decoded = EventCodec.decode(encoded, serializer);

        assertEquals(original, decoded);
    }

    @Test
    void decodeMetadataSkipsPayloadDeserialization() {
        UUID id = UUID.randomUUID();
        Date expiredAt = new Date(123456L);
        Event original = new Event(id, UUID.randomUUID(), "def", UUID.randomUUID(), "ev", "bk", true, expiredAt, "payload");

        byte[] encoded = EventCodec.encode(original, serializer);
        EventMetadata meta = EventCodec.decodeMetadata(encoded);

        assertEquals(id, meta.id());
        assertEquals("bk", meta.processBusinessKey());
        assertEquals("ev", meta.name());
        assertEquals(123456L, meta.expiredAtMillis());
    }

    @Test
    void rejectsUnsupportedVersionAndCorruptRecords() {
        assertNull(EventCodec.decode(null, serializer));

        assertThrows(PersistenceException.class, () -> EventCodec.decode(new byte[]{99, 0}, serializer));
        assertThrows(PersistenceException.class, () -> EventCodec.decode(new byte[]{1, 0, 1, 2}, serializer));

        Event original = new Event(UUID.randomUUID(), null, null, null, "ev", "bk", false, null, null);
        byte[] encoded = EventCodec.encode(original, serializer);
        byte[] withTrailing = new byte[encoded.length + 1];
        System.arraycopy(encoded, 0, withTrailing, 0, encoded.length);
        assertThrows(PersistenceException.class, () -> EventCodec.decode(withTrailing, serializer));
    }

    @Test
    void rejectsNullSerializedBytesForNonNullPayload() {
        Serializer brokenSerializer = new Serializer() {
            @Override
            public byte[] serialize(Object value) {
                return null;
            }

            @Override
            public Object deserialize(byte[] bytes) {
                return null;
            }
        };

        Event ev = new Event(UUID.randomUUID(), null, null, null, "ev", "bk", false, null, "non-null");
        assertThrows(PersistenceException.class, () -> EventCodec.encode(ev, brokenSerializer));
    }
}
