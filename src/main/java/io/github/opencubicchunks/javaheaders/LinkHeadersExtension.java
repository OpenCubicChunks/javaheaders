package io.github.opencubicchunks.javaheaders;

import io.github.opencubicchunks.javaheaders.LinkHeadersTransform.Parameters;

import java.io.File;

public class LinkHeadersExtension {
    private final Parameters parameters;

    public LinkHeadersExtension(Parameters parameters) {
        this.parameters = parameters;
    }

    public void setAcceptedJars(String acceptedJars) {
        this.parameters.setAcceptedJars(acceptedJars);
    }

    public void setConfig(File configFile) {
        this.parameters.setConfig(configFile);
    }

    public void setDebug(boolean value) {
        this.parameters.setDebug(value ? System.nanoTime() : 0);
    }
}
