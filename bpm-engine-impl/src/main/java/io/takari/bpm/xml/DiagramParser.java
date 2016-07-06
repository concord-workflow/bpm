package io.takari.bpm.xml;

import io.takari.bpm.model.diagram.ProcessDiagram;
import java.io.InputStream;

public interface DiagramParser {
    
    ProcessDiagram parse(InputStream in) throws ParserException;
}
