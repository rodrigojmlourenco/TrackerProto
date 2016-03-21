package org.trace.tracking.tracker.exceptions;

/**
 * Created by Rodrigo Lourenço on 18/02/2016.
 */
public class GoogleApiClientDisconnectedException extends RuntimeException {
    @Override
    public String getMessage() {
        return "Google Api Client is disconnected.";
    }
}
