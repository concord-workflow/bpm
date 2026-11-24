package io.takari.bpm.testkit;

import io.takari.bpm.EngineBuilder;
import io.takari.bpm.IndexedProcessDefinition;
import io.takari.bpm.api.Engine;
import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.event.EventPersistenceManager;
import io.takari.bpm.event.EventPersistenceManagerImpl;
import io.takari.bpm.event.InMemEventStorage;
import io.takari.bpm.lock.StripedLockManagerImpl;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.persistence.InMemPersistenceManager;
import io.takari.bpm.xml.Parser;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public class EngineExtension implements BeforeEachCallback, AfterEachCallback {

    private final DeploymentProcessor deploymentProcessor;

    private TestProcessDefinitionProvider processDefinitionProvider;
    private EventPersistenceManager eventManager;
    private Engine engine;

    public EngineExtension(DeploymentProcessor deploymentProcessor) {
        this.deploymentProcessor = deploymentProcessor;
    }

    public EngineExtension(Parser parser) {
        this.deploymentProcessor = (in, provider) -> {
            ProcessDefinition pd = parser.parse(in);
            IndexedProcessDefinition ipd = new IndexedProcessDefinition(pd);
            provider.add(ipd);
        };
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (engine == null) {
            processDefinitionProvider = new TestProcessDefinitionProvider();
            eventManager = new EventPersistenceManagerImpl(new InMemEventStorage());

            engine = new EngineBuilder()
                    .withDefinitionProvider(processDefinitionProvider)
                    .withTaskRegistry(new Mocks.Registry())
                    .withEventManager(eventManager)
                    .withPersistenceManager(new InMemPersistenceManager())
                    .withLockManager(new StripedLockManagerImpl(1))
                    .build();
        }

        Method m = context.getRequiredTestMethod();
        Deployment d = m.getAnnotation(Deployment.class);
        if (d != null) {
            for (String s : d.resources()) {
                try (InputStream in = ClassLoader.getSystemResourceAsStream(s)) {
                    deploymentProcessor.handle(in, processDefinitionProvider);
                }
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
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
