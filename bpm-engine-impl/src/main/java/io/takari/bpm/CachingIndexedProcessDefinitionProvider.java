package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachingIndexedProcessDefinitionProvider extends IndexedProcessDefinitionProvider {

    private final ConcurrentMap<String, IndexedProcessDefinition> cache = new ConcurrentHashMap<>();

    public CachingIndexedProcessDefinitionProvider(ProcessDefinitionProvider delegate) {
        super(delegate);
    }

    @Override
    public IndexedProcessDefinition getById(String id) throws ExecutionException {
        IndexedProcessDefinition cached = cache.get(id);
        if (cached != null) {
            return cached;
        }

        try {
            return cache.computeIfAbsent(id, k -> {
                try {
                    return super.getById(k);
                } catch (ExecutionException e) {
                    throw new UncheckedExecutionException(e);
                }
            });
        } catch (UncheckedExecutionException e) {
            throw new ExecutionException("Error retrieving a definition from the cache", e.getCause());
        }
    }

    private static final class UncheckedExecutionException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private UncheckedExecutionException(ExecutionException cause) {
            super(cause);
        }
    }
}
