package io.takari.bpm.model;

import java.util.Set;

public class ScriptTask extends AbstractElement {

    private final Type type;
    private final String language;
    private final String content;
    private final Set<VariableMapping> in;
    private final Set<VariableMapping> out;
    private boolean copyAllVariables;

    public ScriptTask(String id, Type type, String language, String content) {
        this(id, type, language, content, null, null, false);
    }

    public ScriptTask(String id, Type type, String language, String content, boolean copyAllVariables) {
        this(id, type, language, content, null, null, copyAllVariables);
    }

    public ScriptTask(String id, Type type, String language, String content, Set<VariableMapping> in, Set<VariableMapping> out) {
        this(id, type, language, content, in, out, false);
    }

    public ScriptTask(String id, Type type, String language, String content, Set<VariableMapping> in, Set<VariableMapping> out, boolean copyAllVariables) {
        super(id);
        this.type = type;
        this.language = language;
        this.content = content;
        this.in = in;
        this.out = out;
        this.copyAllVariables = copyAllVariables;
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

    public boolean isCopyAllVariables() {
        return copyAllVariables;
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

    @Override
    public String toString() {
        return "ScriptTask (" + getId() + ") {" +
                "type=" + type +
                ", language='" + language + '\'' +
                ", in=" + in +
                ", out=" + out +
                ", content='" + content + '\'' +
                ", copyAllVariables='" + copyAllVariables + '\'' +
                '}';
    }
}
