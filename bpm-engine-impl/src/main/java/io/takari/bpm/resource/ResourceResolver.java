package io.takari.bpm.resource;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceResolver {

    InputStream getResourceAsStream(String name) throws IOException;
}
