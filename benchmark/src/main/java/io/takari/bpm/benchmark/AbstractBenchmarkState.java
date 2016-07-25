package io.takari.bpm.benchmark;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.takari.bpm.el.ScriptingExpressionManager;
import io.takari.bpm.task.KeyAwareServiceTaskRegistry;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.openjdk.jmh.annotations.TearDown;

import com.google.common.io.Files;

import io.takari.bpm.EngineBuilder;
import io.takari.bpm.ProcessDefinitionProvider;
import io.takari.bpm.api.Engine;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.leveldb.Configuration;
import io.takari.bpm.leveldb.KryoSerializer;
import io.takari.bpm.leveldb.LevelDbEventStorage;
import io.takari.bpm.leveldb.Serializer;
import io.takari.bpm.lock.LockManager;
import io.takari.bpm.lock.StripedLockManagerImpl;
import io.takari.bpm.mapdb.MapDbPersistenceManager;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.persistence.PersistenceManager;
import io.takari.bpm.task.ServiceTaskRegistry;

public abstract class AbstractBenchmarkState {
    
    private final Engine engine;
    private final DummyServiceTaskRegistry serviceTaskRegistry;
    private LevelDbEventStorage levelDbEventStorage;
    private MapDbPersistenceManager mapDbPersistenceManager;
    
    public AbstractBenchmarkState(ProcessDefinition def) {
        this(true, false, def);
    }
    
    public AbstractBenchmarkState(boolean inMem, boolean nashorn, ProcessDefinition def) {
        this.serviceTaskRegistry = new DummyServiceTaskRegistry();
        
        DummyProcessDefinitionProvider defs = new DummyProcessDefinitionProvider();
        defs.publish(def.getId(), def);
        
        LockManager lockManager = new StripedLockManagerImpl(65536);
        EventPersistenceManager eventPersistenceManager;
        PersistenceManager persistenceManager;
        
        if (inMem) {
            eventPersistenceManager = new EventPersistenceManagerImpl(new InMemEventStorage());
            persistenceManager = new InMemPersistenceManager();
        } else {
            String baseDir;
            try {
                File f = Files.createTempDir();
                f.mkdirs();
                baseDir = f.getAbsolutePath();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            Configuration cfg = new Configuration();
            cfg.setEventPath(baseDir + "/events");
            cfg.setExecutionPath(baseDir + "/executions");
            cfg.setExpiredEventIndexPath(baseDir + "/exprired");
            cfg.setBusinessKeyEventIndexPath(baseDir + "/bki");
            
            DBFactory dbf = new Iq80DBFactory();
            
            Serializer serializer = new KryoSerializer();
            
            levelDbEventStorage = new LevelDbEventStorage(cfg, dbf, serializer);
            levelDbEventStorage.init();
            eventPersistenceManager = new EventPersistenceManagerImpl(levelDbEventStorage);
            
            mapDbPersistenceManager = new MapDbPersistenceManager();
            mapDbPersistenceManager.setBaseDir(baseDir + "/executions");
            mapDbPersistenceManager.start();
            persistenceManager = mapDbPersistenceManager;
        }

        EngineBuilder builder = new EngineBuilder()
                .withDefinitionProvider(defs)
                .withTaskRegistry(serviceTaskRegistry)
                .withEventManager(eventPersistenceManager)
                .withPersistenceManager(persistenceManager)
                .withLockManager(lockManager);

        if (nashorn) {
            builder.withExpressionManager(new ScriptingExpressionManager("nashorn", serviceTaskRegistry));
        }

        this.engine = builder.build();
    }
    
    @TearDown
    public void close() {
        if (levelDbEventStorage != null) {
            levelDbEventStorage.close();
        }
        
        if (mapDbPersistenceManager != null) {
            mapDbPersistenceManager.stop();
        }
    }

    public final Engine getEngine() {
        return engine;
    }

    public final DummyServiceTaskRegistry getServiceTaskRegistry() {
        return serviceTaskRegistry;
    }
    
    public static class DummyProcessDefinitionProvider implements ProcessDefinitionProvider {

        private final Map<String, ProcessDefinition> defs = new HashMap<>();

        public void publish(String id, ProcessDefinition def) {
            defs.put(id, def);
        }

        @Override
        public ProcessDefinition getById(String id) throws ExecutionException {
            return defs.get(id);
        }
    }

    public static class DummyServiceTaskRegistry implements KeyAwareServiceTaskRegistry {

        private final Map<String, Object> tasks = new HashMap<>();
        
        public void register(String key, Object instance) {
            tasks.put(key, instance);
        }

        @Override
        public Object getByKey(String key) {
            return tasks.get(key);
        }

        @Override
        public boolean containsKey(String key) {
            return tasks.containsKey(key);
        }
    }
}
