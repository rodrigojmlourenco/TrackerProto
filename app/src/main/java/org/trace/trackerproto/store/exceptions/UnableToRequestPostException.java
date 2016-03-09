package org.trace.trackerproto.store.exceptions;

/**
 * Created by Rodrigo Lourenço on 09/03/2016.
 */
public class UnableToRequestPostException extends Throwable {

    private String message;

    public UnableToRequestPostException(String cause){
        message = "Unable to fulfil the POST request because "+cause;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
