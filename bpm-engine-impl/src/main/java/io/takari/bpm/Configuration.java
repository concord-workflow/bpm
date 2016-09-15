package io.takari.bpm;

import java.io.Serializable;

public class Configuration implements Serializable {

    private boolean throwExceptionOnErrorEnd = false;

    public boolean isThrowExceptionOnErrorEnd() {
        return throwExceptionOnErrorEnd;
    }

    public void setThrowExceptionOnErrorEnd(boolean throwExceptionOnErrorEnd) {
        this.throwExceptionOnErrorEnd = throwExceptionOnErrorEnd;
    }
}
