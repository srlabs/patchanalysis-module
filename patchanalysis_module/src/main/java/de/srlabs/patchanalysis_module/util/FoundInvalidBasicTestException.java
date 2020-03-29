package de.srlabs.patchanalysis_module.util;

public class FoundInvalidBasicTestException extends Exception {

    public FoundInvalidBasicTestException(String message) {
        super(message);
    }

    public FoundInvalidBasicTestException(Throwable cause) {
        super(cause);
    }

    public FoundInvalidBasicTestException(String message, Throwable cause) {
        super(message, cause);
    }

}
