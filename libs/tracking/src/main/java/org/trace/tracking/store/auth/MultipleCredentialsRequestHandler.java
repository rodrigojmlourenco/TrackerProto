package org.trace.tracking.store.auth;

import android.content.Intent;
import android.os.Trace;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;

import org.trace.tracking.store.TraceAuthenticationManager;

public class MultipleCredentialsRequestHandler {

    private static final String TAG = "Auth";

    private GoogleApiClient mGoogleApiClient;
    private TraceAuthenticationManager mAuthManager;

    public MultipleCredentialsRequestHandler(GoogleApiClient googleApiClient, TraceAuthenticationManager manager){
        mAuthManager = manager;
        mGoogleApiClient = googleApiClient;
    }

    public int onRequestResult(int requestCode, int resultCode, Intent data){
        if(resultCode != -1) {
            Log.e(TAG, "Failed at code " + requestCode);
            return -1;
        }

        switch (requestCode){
            case TraceAuthenticationManager.RC_SIGN_IN:
                googleSignIn(data);
                break;
            case TraceAuthenticationManager.RC_LOAD:
                loginFromStoredCredentials(data);
                break;
            case TraceAuthenticationManager.RC_SAVE:
                Log.d(TAG, "TODO: Save - dont know what to do");
                break;
            case TraceAuthenticationManager.RC_DELETE:
                removeCredential(data);
                break;
            default:
                Log.e(TAG, "Unknown request code "+requestCode);
        }

        return requestCode;
    }

    private void googleSignIn(Intent data){
        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

        if (result.isSuccess()) {
            GoogleSignInAccount acct = result.getSignInAccount();
            mAuthManager.login(acct, TraceAuthenticationManager.GrantType.google);
        }else{
            Log.e(TAG, "Failed to handle sign in request");
        }
    }
    
    private void loginFromStoredCredentials(Intent data){

        Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
        String accountType = credential.getAccountType();

        if(accountType == null){ //Login from stored password
            mAuthManager.login(credential.getId(), credential.getPassword());
        }else if(accountType.equals(IdentityProviders.GOOGLE)){

            //Login with google - Silent Login
            OptionalPendingResult<GoogleSignInResult> opr =
                    Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {


                    mAuthManager.login(googleSignInResult.getSignInAccount(), TraceAuthenticationManager.GrantType.google);
                }
            });
        }else{
            Log.e(TAG, "Unsupported provider '"+accountType+"'");
        }
    }

    private void removeCredential(Intent data){
        Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
        mAuthManager.removeCredential(credential);
    }
}
