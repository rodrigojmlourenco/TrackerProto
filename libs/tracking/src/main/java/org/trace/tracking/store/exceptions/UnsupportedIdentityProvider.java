package org.trace.tracking.store.exceptions;

/**
 * Created by Rodrigo Lourenço on 01/04/2016.
 */
public class UnsupportedIdentityProvider extends RuntimeException {

    private String message;

    public UnsupportedIdentityProvider(String identityProvider){
        message = "Unsupported Identity Provider '"+identityProvider+"'.";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
