package com.academicpassport.marksheet.provider;

public class ExtractionException extends RuntimeException {
    
    private final boolean isPermanent;

    public ExtractionException(String message, boolean isPermanent) {
        super(message);
        this.isPermanent = isPermanent;
    }

    public ExtractionException(String message, Throwable cause, boolean isPermanent) {
        super(message, cause);
        this.isPermanent = isPermanent;
    }

    public boolean isPermanent() {
        return isPermanent;
    }
}
