package org.trace.trackerproto.store.exceptions;

/**
 * Created by Rodrigo Lourenço on 22/02/2016.
 */
public class InvalidAuthCredentialsException extends RuntimeException {

    @Override
    public String getMessage() {
        return "The provided username and password are not valid.";
    }
}
