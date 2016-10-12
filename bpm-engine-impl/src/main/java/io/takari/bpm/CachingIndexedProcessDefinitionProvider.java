package io.takari.bpm;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.takari.bpm.api.ExecutionException;

public class CachingIndexedProcessDefinitionProvider extends IndexedProcessDefinitionProvider {

    private final Cache<String, IndexedProcessDefinition> cache = CacheBuilder.newBuilder().build();

    public CachingIndexedProcessDefinitionProvider(ProcessDefinitionProvider delegate) {
        super(delegate);
    }

    @Override
    public IndexedProcessDefinition getById(String id) throws ExecutionException {
        try {
            return cache.get(id, () -> CachingIndexedProcessDefinitionProvider.super.getById(id));
        } catch (java.util.concurrent.ExecutionException e) {
            throw new ExecutionException("Error retrieving a definition from the cache", e);
        }
    }
}
