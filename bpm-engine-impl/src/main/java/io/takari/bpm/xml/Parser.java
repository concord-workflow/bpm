package io.takari.bpm.xml;

import java.io.InputStream;

import io.takari.bpm.model.ProcessDefinition;

public interface Parser {

    ProcessDefinition parse(InputStream in) throws ParserException;
}
