package io.takari.bpm.leveldb;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class KryoSerializerTest {

    @Test
    public void hashSetSerialize() throws Exception {
        KryoSerializer s = new KryoSerializer();

        Set<String> eventNames = new HashSet<>(Arrays.asList("a", "b", "c"));

        byte[] bytes = s.toBytes(eventNames);
        assertNotNull(bytes);

        assertEquals(eventNames, s.fromBytes(bytes));
    }

    private static class Service {
        private final String sid;

        public Service(String sid) {
            this.sid = sid;
        }
    }
}
