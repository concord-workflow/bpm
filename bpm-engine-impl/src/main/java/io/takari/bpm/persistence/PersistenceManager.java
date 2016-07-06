package io.takari.bpm.persistence;

import io.takari.bpm.DefaultExecution;
import java.util.UUID;

public interface PersistenceManager {

    void save(DefaultExecution execution);
    
    DefaultExecution get(UUID id);

    DefaultExecution remove(UUID id);
}
