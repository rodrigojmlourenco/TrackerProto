package org.trace.tracking.store.exceptions;

/**
 * Created by Rodrigo Lourenço on 30/03/2016.
 */
public class MissingCredentialsApiException extends RuntimeException{
    @Override
    public String getMessage() {
        return "The provided GoogleApiClient must contemplate the Credentials Api.";
    }
}
