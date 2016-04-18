package org.trace.storeclient;

public interface StoreClientConstants {

    String FIRST_TIME_BROADCAST = "org.trace.intent.FIRST_TIME";

    //Login
    String LOGIN_ACTION = "org.trace.intent.LOGIN";
    String SUCCESS_LOGIN_EXTRA = "org.trace.intent.SUCCESS_LOGIN";
    String LOGIN_ERROR_MSG_EXTRA = "org.trace.intent.LOGIN_ERROR";

    //AuthTokenExpired
    String TOKEN_EXPIRED_ACTION  = "org.trace.intent.EXPIRED_TOKEN";
    String FAILED_OPERATION_KEY = "org.trace.intent.FAILED_OPERATION";
    String AUTH_TOKEN_EXTRA = "auth_token";
    String TRACK_EXTRA  = "org.trace.store.extras.TRACK";
    String OPERATION_KEY    = "action";
}
