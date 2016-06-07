package org.trace.storeclient.exceptions;

//TODO: o requestType devia ser o error code
public class RemoteTraceException extends Throwable {

    private String message;

    private int requestType;
    private String cause;

    public RemoteTraceException(String requestType, String cause){
        this.cause = cause;
        message = requestType+" request failed because "+cause;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public int getErrorCode(){
        return requestType;
    }

    public String getErrorMessage(){
        return cause;
    }
}
