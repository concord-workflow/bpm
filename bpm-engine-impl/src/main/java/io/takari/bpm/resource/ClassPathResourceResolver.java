package io.takari.bpm.resource;

import java.io.IOException;
import java.io.InputStream;

public class ClassPathResourceResolver implements ResourceResolver {

    @Override
    public InputStream getResourceAsStream(String name) throws IOException {
        return ClassLoader.getSystemResourceAsStream(name);
    }
}
