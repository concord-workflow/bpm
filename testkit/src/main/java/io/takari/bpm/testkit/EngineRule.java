package io.takari.bpm.testkit;

import io.takari.bpm.EngineBuilder;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.api.Engine;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.leveldb.Configuration;
import io.takari.bpm.leveldb.LevelDbPersistenceManager;
import io.takari.bpm.lock.StripedLockManagerImpl;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.xml.Parser;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import io.takari.bpm.xml.ParserException;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class EngineRule implements TestRule {

    private final DeploymentProcessor deploymentProcessor;

    private TestProcessDefinitionProvider processDefinitionProvider;
    private EventPersistenceManager eventManager;
    private Engine engine;

    public EngineRule(DeploymentProcessor deploymentProcessor) {
        this.deploymentProcessor = deploymentProcessor;
    }

    public EngineRule(Parser parser) {
        this.deploymentProcessor = (in, provider) -> {
            ProcessDefinition pd = parser.parse(in);
            IndexedProcessDefinition ipd = new IndexedProcessDefinition(pd);
            provider.add(ipd);
        };
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before(description);
                try {
                    base.evaluate();
                } finally {
                    after(description);
                }
            }
        };
    }

    protected void before(Description description) throws Exception {
        if (engine == null) {
            processDefinitionProvider = new TestProcessDefinitionProvider();
            eventManager = new EventPersistenceManagerImpl(new InMemEventStorage());
            Configuration cfg = new Configuration();
            cfg.setExecutionPath("/tmp/bpm/" + System.currentTimeMillis());
            DBFactory f = new Iq80DBFactory();
            LevelDbPersistenceManager levelDbPersistenceManager = new LevelDbPersistenceManager(cfg, f	);
            levelDbPersistenceManager.init();
            
            engine = new EngineBuilder()
                    .withDefinitionProvider(processDefinitionProvider)
                    .withTaskRegistry(new Mocks.Registry())
                    .withEventManager(eventManager)
                    .withPersistenceManager(levelDbPersistenceManager)
                    .withLockManager(new StripedLockManagerImpl(1))
                    .build();
        }

        Class<?> k = description.getTestClass();
        String n = description.getMethodName();
        for (Method m : k.getDeclaredMethods()) {
            if (m.getName().equals(n)) {
                Deployment d = m.getAnnotation(Deployment.class);
                if (d != null) {
                    for (String s : d.resources()) {
                        InputStream in = ClassLoader.getSystemResourceAsStream(s);
                        deploymentProcessor.handle(in, processDefinitionProvider);
                    }
                }
            }
        }
    }

    protected void after(Description description) {
        engine = null;
    }

    public String startProcessInstanceByKey(String key, Map<String, Object> input) throws ExecutionException {
        String id = UUID.randomUUID().toString();
        startProcessInstanceByKey(id, key, input);
        return id;
    }

    public void startProcessInstanceByKey(String txId, String key, Map<String, Object> input) throws ExecutionException {
        engine.start(txId, key, input);
    }

    public void wakeUp(String key, String eventId) throws ExecutionException {
        wakeUp(key, eventId, null);
    }

    public void wakeUp(String key, String eventId, Map<String, Object> variables) throws ExecutionException {
        engine.resume(key, eventId, variables);
    }

    public EventPersistenceManager getEventManager() {
        return eventManager;
    }

    public Engine getEngine() {
        return engine;
    }
}
