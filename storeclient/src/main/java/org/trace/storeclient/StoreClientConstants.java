package org.trace.storeclient;

public interface StoreClientConstants {

    String FIRST_TIME_BROADCAST = "org.trace.intent.FIRST_TIME";

    //Login
    String LOGIN_ACTION = "org.trace.intent.LOGIN";
    String SUCCESS_LOGIN_EXTRA = "org.trace.intent.SUCCESS_LOGIN";
    String LOGIN_ERROR_MSG_EXTRA = "org.trace.intent.LOGIN_ERROR";

    //AuthTokenExpired
    String TOKEN_EXPIRED_ACTION  = "org.trace.intent.EXPIRED_TOKEN";
    String FAILED_OPERATION = "org.trace.intent.FAILED_OPERATION";
    String AUTH_TOKEN_EXTRA = "auth_token";
    String TRACK_EXTRA  = "org.trace.store.extras.TRACK";
    String TRACE_EXTRA  = "org.trace.store.extras.TRACE";
    String USER_REQUEST_EXTRA  = "org.trace.store.extras.REGISTER_USER";
    String OPERATION_EXTRA  = "org.trace.store.extras.OPERATION";
    String ERROR_CODE_EXTRA  = "org.trace.store.extras.ERROR_CODE";
    String ERROR_MSG_EXTRA  = "org.trace.store.extras.ERROR_MSG";
    String OPERATION_KEY    = "action";
    String LATITUDE = "org.trace.store.extras.LATITUDE";
    String LONGITUDE = "org.trace.store.extras.LONGITUDE";
    String RADIUS = "org.trace.store.extras.RADIUS";

    interface auth {
        String CREDENTIAL_DELETED = "org.trace.intent.CREDENTIAL_DELETED";
        String CREDENTIAL_STORED = "org.trace.intent.CREDENTIAL_STORED";
        String LOGOUT = "org.trace.intent.CREDENTIAL_LOGOUT";

        String SUCCESS = "org.trace.extra.SUCCESS";
        String STATUS = "org.trace.extra.STATUS";
    }

    interface register_user {
        String NAME = "name";
        String USERNAME = "username";
        String EMAIL = "email";
        String PASSWORD = "password";
        String CONFIRMATION_PASSWORD = "confirm";
        String PHONE = "phone";
        String ADDRESS = "address";
    }

    interface response {
        String  SUCCESS = "success",
                ERROR_CODE = "code",
                ERROR_MSG = "error";

        String PAYLOAD = "payload";
    }


}
