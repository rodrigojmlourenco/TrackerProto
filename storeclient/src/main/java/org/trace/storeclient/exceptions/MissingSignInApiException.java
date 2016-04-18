package org.trace.storeclient.exceptions;

/**
 * Created by Rodrigo Lourenço on 30/03/2016.
 */
public class MissingSignInApiException extends RuntimeException {

    @Override
    public String getMessage() {
        return "The provided GoogleApiClient must contemplate the Sign In Api.";
    }
}
