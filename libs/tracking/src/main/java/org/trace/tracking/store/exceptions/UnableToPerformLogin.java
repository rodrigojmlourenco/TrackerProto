package org.trace.tracking.store.exceptions;


public class UnableToPerformLogin extends Throwable {

    private String message;

    public UnableToPerformLogin(String message){
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
