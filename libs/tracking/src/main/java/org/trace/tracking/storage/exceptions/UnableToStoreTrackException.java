package org.trace.tracking.storage.exceptions;

/**
 * Created by Rodrigo Lourenço on 22/02/2016.
 */
public class UnableToStoreTrackException extends Exception {

    private String message;

    public UnableToStoreTrackException(String cause){
        this.message = "Unable to store track because: "+cause;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
