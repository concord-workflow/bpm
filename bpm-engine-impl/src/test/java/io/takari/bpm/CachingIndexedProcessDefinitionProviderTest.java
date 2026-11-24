package io.takari.bpm;

import io.takari.bpm.api.ExecutionException;
import io.takari.bpm.model.EndEvent;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.SequenceFlow;
import io.takari.bpm.model.StartEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachingIndexedProcessDefinitionProviderTest {

    @Test
    public void testCachesDefinition() throws Exception {
        ProcessDefinition pd = new ProcessDefinition("p1",
                new StartEvent("s"),
                new SequenceFlow("f1", "s", "e"),
                new EndEvent("e"));

        ProcessDefinitionProvider delegate = mock(ProcessDefinitionProvider.class);
        when(delegate.getById("p1")).thenReturn(pd);

        CachingIndexedProcessDefinitionProvider provider = new CachingIndexedProcessDefinitionProvider(delegate);
        IndexedProcessDefinition first = provider.getById("p1");
        IndexedProcessDefinition second = provider.getById("p1");

        assertSame(first, second);
        verify(delegate, times(1)).getById("p1");
    }

    @Test
    public void testPropagatesExecutionException() throws Exception {
        ProcessDefinitionProvider delegate = mock(ProcessDefinitionProvider.class);
        when(delegate.getById("missing")).thenThrow(new ExecutionException("not found"));

        CachingIndexedProcessDefinitionProvider provider = new CachingIndexedProcessDefinitionProvider(delegate);
        assertThrows(ExecutionException.class, () -> provider.getById("missing"));
    }
}
