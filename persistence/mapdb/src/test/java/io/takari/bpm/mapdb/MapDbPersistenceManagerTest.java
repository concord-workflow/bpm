package io.takari.bpm.mapdb;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.state.ProcessInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class MapDbPersistenceManagerTest {

    private MapDbPersistenceManager manager;

    @Before
    public void setUp() throws Exception {
        manager = new MapDbPersistenceManager();
        manager.setBaseDir(Files.createTempDirectory("mapdb").toAbsolutePath().toString());
        manager.start();
    }

    @After
    public void tearDown() {
        manager.stop();
    }

    @Test
    public void testRoundtrip() throws Exception {
        UUID id = UUID.randomUUID();
        String key = "abc";
        IndexedProcessDefinition pd = new IndexedProcessDefinition(new ProcessDefinition("test"));

        ProcessInstance p1 = new ProcessInstance(id, key, pd);
        manager.save(p1);

        ProcessInstance p2 = manager.get(id);
        assertEquals(p1.getBusinessKey(), p2.getBusinessKey());
    }
}
