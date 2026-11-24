package io.takari.bpm.mvstore;

import org.junit.jupiter.api.Test;

import java.io.ObjectInputFilter;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JavaSerializerTest {

    record SampleRecord(String name, int count) implements Serializable {
    }

    @Test
    void handlesNullValues() {
        JavaSerializer serializer = new JavaSerializer();
        assertNull(serializer.serialize(null));
        assertNull(serializer.deserialize(null));
    }

    @Test
    void roundTripsSerializableObjects() {
        JavaSerializer serializer = new JavaSerializer();
        Map<String, Object> original = Map.of(
                "text", "hello",
                "list", List.of(1, 2, 3),
                "rec", new SampleRecord("abc", 42)
        );

        byte[] bytes = serializer.serialize(original);
        assertNotNull(bytes);
        assertEquals(original, serializer.deserialize(bytes));
    }

    @Test
    void enforcesObjectInputFilter() {
        JavaSerializer writer = new JavaSerializer();
        byte[] bytes = writer.serialize(new SampleRecord("blocked", 1));

        ObjectInputFilter denyAll = info -> ObjectInputFilter.Status.REJECTED;
        JavaSerializer strictReader = new JavaSerializer(denyAll);
        assertThrows(PersistenceException.class, () -> strictReader.deserialize(bytes));

        ObjectInputFilter allowSample = ObjectInputFilter.Config.createFilter(
                SampleRecord.class.getName() + ";!*");
        JavaSerializer allowedReader = new JavaSerializer(allowSample);
        assertEquals(new SampleRecord("blocked", 1), allowedReader.deserialize(bytes));
    }

    @Test
    void throwsOnNonSerializableObject() {
        JavaSerializer serializer = new JavaSerializer();
        assertThrows(PersistenceException.class, () -> serializer.serialize(new Object()));
    }

    @Test
    void throwsOnCorruptBytes() {
        JavaSerializer serializer = new JavaSerializer();
        assertThrows(PersistenceException.class, () -> serializer.deserialize(new byte[]{1, 2, 3, 4}));
    }
}
