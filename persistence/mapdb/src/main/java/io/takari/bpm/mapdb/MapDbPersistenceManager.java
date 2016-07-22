package io.takari.bpm.mapdb;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.state.ProcessInstance;

public class MapDbPersistenceManager implements PersistenceManager {
    
    private String baseDir = "/tmp/";
    private DB db;
    private Map<UUID, ProcessInstance> store;

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public synchronized void start() {
        File f = new File(baseDir);
        f.mkdirs();

        f = new File(baseDir + "/db");
        db = DBMaker.newFileDB(f)
                .transactionDisable()
                .mmapFileEnableIfSupported()
                .make();

        store = db.getHashMap("executions");
    }

    public synchronized void stop() {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    @Override
    public void save(ProcessInstance execution) {
        UUID id = execution.getId();
        store.put(id, execution);
    }

    @Override
    public ProcessInstance get(UUID id) {
        return store.get(id);
    }

    @Override
    public void remove(UUID id) {
        store.remove(id);
    }
}
