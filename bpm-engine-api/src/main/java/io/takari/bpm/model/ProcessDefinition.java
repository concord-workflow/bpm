package io.takari.bpm.model;

import java.util.*;

public class ProcessDefinition extends AbstractElement {

    private static final long serialVersionUID = 1L;

    public static final String SOURCE_TYPE_ATTRIBUTE = "sourceType";
    public static final String ATTRIBUTE_KEY_PREFIX = "__attr_";

    private String name;

    private final Map<String, AbstractElement> entities;
    private final Map<String, String> attributes;

    public ProcessDefinition(ProcessDefinition source, Map<String, String> attributes) {
        this(source.getId(), source.getChildren(), attributes);
    }

    public ProcessDefinition(String id, Collection<AbstractElement> children) {
        this(id, children, Collections.emptyMap());
    }

    public ProcessDefinition(String id, Collection<AbstractElement> children, Map<String, String> attributes) {
        super(id);

        Map<String, AbstractElement> m = new LinkedHashMap<>();
        if (children != null) {
            for (AbstractElement c : children) {
                m.put(c.getId(), c);
            }
        }

        this.entities = Collections.unmodifiableMap(m);
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public AbstractElement getChild(String id) {
        return entities.get(id);
    }

    public boolean hasChild(String id) {
        return entities.containsKey(id);
    }

    public Collection<AbstractElement> getChildren() {
        return entities.values();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
