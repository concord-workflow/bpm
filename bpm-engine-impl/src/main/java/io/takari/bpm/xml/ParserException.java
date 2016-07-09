package io.takari.bpm.xml;

public class ParserException extends Exception {
	
	private static final long serialVersionUID = 1L;

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
