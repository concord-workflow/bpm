package io.takari.bpm.xml;

import java.io.InputStream;

import io.takari.bpm.model.diagram.ProcessDiagram;

public interface DiagramParser {
    
    ProcessDiagram parse(InputStream in) throws ParserException;
}
