package io.takari.bpm.mvstore;

import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.api.Variables;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.StartEvent;
import io.takari.bpm.state.ProcessInstance;
import io.takari.bpm.state.ProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MvStorePersistenceManagerTest {

    @TempDir
    Path tempDir;

    private static ProcessInstance createInstance(UUID id, String businessKey, String varValue) {
        ProcessDefinition pd = new ProcessDefinition("test-proc", List.of(
                new StartEvent("start"),
                new SequenceFlow("f1", "start", "end"),
                new EndEvent("end")
        ));
        IndexedProcessDefinition indexed = new IndexedProcessDefinition(pd);
        Variables vars = new Variables(Map.of("k", varValue, "n", 42));
        return new ProcessInstance(id, businessKey, indexed, vars);
    }

    @Test
    void crudOperationsInMemory() throws Exception {
        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.inMemory().build())) {
            MvStorePersistenceManager pm = db.persistenceManager();
            UUID id = UUID.randomUUID();

            assertNull(pm.get(id));

            ProcessInstance initial = createInstance(id, "bk-1", "v1");
            pm.save(initial);

            ProcessInstance loaded = pm.get(id);
            assertNotNull(loaded);
            assertEquals(id, loaded.getId());
            assertEquals("bk-1", loaded.getBusinessKey());
            assertEquals("v1", loaded.getVariables().getVariable("k"));
            assertEquals(42, loaded.getVariables().getVariable("n"));

            ProcessInstance updated = loaded.setStatus(ProcessStatus.SUSPENDED);
            pm.save(updated);
            assertEquals(ProcessStatus.SUSPENDED, pm.get(id).getStatus());

            pm.remove(id);
            assertNull(pm.get(id));
        }
    }

    @Test
    void persistsAcrossCloseAndReopenWithCompression() throws Exception {
        Path dbFile = tempDir.resolve("nested/state.mv");
        UUID id = UUID.randomUUID();

        try (MvStoreDatabase db = MvStoreDatabase.open(
                MvStoreConfig.file(dbFile).compress(true).syncWrite(true).build())) {
            db.persistenceManager().save(createInstance(id, "bk-disk", "persisted"));
            db.compact(100);
        }

        try (MvStoreDatabase reopened = MvStoreDatabase.open(
                MvStoreConfig.file(dbFile).compress(true).build())) {
            ProcessInstance loaded = reopened.persistenceManager().get(id);
            assertNotNull(loaded);
            assertEquals(id, loaded.getId());
            assertEquals("bk-disk", loaded.getBusinessKey());
            assertEquals("persisted", loaded.getVariables().getVariable("k"));
        }
    }

    @Test
    void readOnlyModeAllowsReadsAndRejectsWrites() throws Exception {
        Path dbFile = tempDir.resolve("ro.mv");
        UUID id = UUID.randomUUID();

        try (MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.file(dbFile).build())) {
            db.persistenceManager().save(createInstance(id, "bk-ro", "val"));
        }

        try (MvStoreDatabase ro = MvStoreDatabase.open(MvStoreConfig.file(dbFile).readOnly(true).build())) {
            assertNotNull(ro.persistenceManager().get(id));
            assertThrows(ExecutionException.class,
                    () -> ro.persistenceManager().save(createInstance(UUID.randomUUID(), "bk-2", "v")));
            assertThrows(PersistenceException.class, () -> ro.persistenceManager().remove(id));
            assertThrows(PersistenceException.class, () -> ro.compact(10));
        }
    }

    @Test
    void rejectsOperationsAfterClose() {
        MvStoreDatabase db = MvStoreDatabase.open(MvStoreConfig.inMemory().build());
        MvStorePersistenceManager pm = db.persistenceManager();
        db.close();
        db.close(); // idempotent

        assertTrue(db.isClosed());
        assertThrows(IllegalStateException.class, () -> pm.get(UUID.randomUUID()));
    }
}
