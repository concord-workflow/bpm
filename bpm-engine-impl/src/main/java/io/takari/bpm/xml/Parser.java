package io.takari.bpm.xml;

import io.takari.bpm.model.ProcessDefinition;
import java.io.InputStream;

public interface Parser {

    ProcessDefinition parse(InputStream in) throws ParserException;
}
