package org.trace.tracking.tracker.storage.exceptions;

/**
 * Created by Rodrigo Lourenço on 22/02/2016.
 */
public class UnableToLoadStoredTrackException extends Exception {

    private String message;

    public UnableToLoadStoredTrackException(String cause){
        this.message = "Unable to load stored track because: "+cause;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
