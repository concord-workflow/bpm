package io.takari.bpm.testkit;

import io.takari.bpm.xml.ParserException;

import java.io.InputStream;

public interface DeploymentProcessor {

    void handle(InputStream in, TestProcessDefinitionProvider provider) throws ParserException;
}
