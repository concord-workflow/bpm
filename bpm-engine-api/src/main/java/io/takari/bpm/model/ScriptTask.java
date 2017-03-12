package io.takari.bpm.model;

import java.util.Set;

public class ScriptTask extends AbstractElement {

    private final Type type;
    private final String language;
    private final String content;
    private final Set<VariableMapping> in;
    private final Set<VariableMapping> out;

    public ScriptTask(String id, Type type, String language, String content) {
        this(id, type, language, content, null, null);
    }

    public ScriptTask(String id, Type type, String language, String content, Set<VariableMapping> in, Set<VariableMapping> out) {
        super(id);
        this.type = type;
        this.language = language;
        this.content = content;
        this.in = in;
        this.out = out;
    }

    public Type getType() {
        return type;
    }

    public String getLanguage() {
        return language;
    }

    public String getContent() {
        return content;
    }

    public Set<VariableMapping> getIn() {
        return in;
    }

    public Set<VariableMapping> getOut() {
        return out;
    }

    public enum Type {
        /**
         * Contains a reference to an external script.
         */
        REFERENCE,

        /**
         * Contains script itself.
         */
        CONTENT
    }
}
