package org.trace.tracking.store.exceptions;

/**
 * Created by Rodrigo Lourenço on 30/03/2016.
 */
public class NotOnConnectionFailedListenerException extends RuntimeException{
    @Override
    public String getMessage() {
        return "The provided context must be an activity that extends the GoogleApiClient.OnConnectionFailedListener interface.";
    }
}
