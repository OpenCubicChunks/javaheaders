package io.github.opencubicchunks.javaheaders.exceptions;

import org.gradle.api.InvalidUserDataException;

public class HeaderNotImplementedException extends InvalidUserDataException {
    public HeaderNotImplementedException() {
        super();
    }

    public HeaderNotImplementedException(String message) {
        super(message);
    }

    public HeaderNotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }
}