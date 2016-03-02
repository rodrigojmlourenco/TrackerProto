package org.trace.trackerproto.store.exceptions;

public class RemoteTraceException extends Throwable {

    private String message;

    public RemoteTraceException(String requestType, String cause){
        message = requestType+" request failed because "+cause;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
