package org.trace.storeclient.exceptions;


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
