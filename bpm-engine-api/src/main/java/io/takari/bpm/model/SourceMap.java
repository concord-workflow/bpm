package io.takari.bpm.model;

import java.io.Serializable;

public class SourceMap implements Serializable {

    private final Significance significance;
    private final String source;
    private final int line;
    private final int column;
    private final String description;

    public SourceMap(Significance significance, String source, int line, int column, String description) {
        this.significance = significance;
        this.source = source;
        this.line = line;
        this.column = column;
        this.description = description;
    }

    public Significance getSignificance() {
        return significance;
    }

    public String getSource() {
        return source;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getDescription() {
        return description;
    }

    public enum Significance {

        HIGH,
        MEDIUM,
        LOW
    }
}
