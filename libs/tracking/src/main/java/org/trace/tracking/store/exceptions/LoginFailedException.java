package org.trace.tracking.store.exceptions;


public class LoginFailedException extends Exception {
    private String message;

    public LoginFailedException(String cause){
        message = "Login failed because "+cause;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
