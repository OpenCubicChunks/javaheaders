package io.github.opencubicchunks.javaheaders;

import io.github.opencubicchunks.javaheaders.LinkHeadersTransform.Parameters;

import java.util.Set;

public class LinkHeadersExtension {
    private final Parameters parameters;

    public LinkHeadersExtension(Parameters parameters) {
        this.parameters = parameters;
    }

    public void setTargetJarNames(Set<String> targetJarNames) {
        this.parameters.setTargetJarNames(targetJarNames);
    }

    public void setDebug(boolean value) {
        this.parameters.setDebug(value ? System.nanoTime() : 0);
    }
}
