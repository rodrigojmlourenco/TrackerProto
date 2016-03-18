package org.trace.tracking.exceptions;

/**
 * Created by Rodrigo Lourenço on 18/02/2016.
 */
public class UnableToParseTraceException  extends Exception{

    private String message;

    public UnableToParseTraceException(String cause){
        message = "Unable to parse trace into GPS Exchange Format because, "+ cause;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
