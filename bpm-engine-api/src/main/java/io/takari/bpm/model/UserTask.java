package io.takari.bpm.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class UserTask extends AbstractElement {

    private static final long serialVersionUID = 1L;

    private final List<Extension> extensions;

    public UserTask(String id) {
        this(id, (List<Extension>) null);
    }

    public UserTask(String id, List<Extension> extensions) {
        super(id);
        this.extensions = extensions;
    }

    public UserTask(String id, Extension... extensions) {
        this(id, Arrays.asList(extensions));
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

    public interface Extension extends Serializable {
    }
}
