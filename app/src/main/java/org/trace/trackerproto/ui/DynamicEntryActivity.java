package org.trace.trackerproto.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;

import org.trace.tracking.store.TraceAuthenticationManager;
import org.trace.tracking.store.auth.LoginBroadcastListener;
import org.trace.tracking.store.auth.MultipleCredentialsRequestHandler;

public class DynamicEntryActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{


    private LoginBroadcastListener mListener;
    private GoogleApiClient mGoogleApiClient;
    private TraceAuthenticationManager mAuthManager;
    private MultipleCredentialsRequestHandler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.CREDENTIALS_API)
                .addApi(Auth.GOOGLE_SIGN_IN_API, TraceAuthenticationManager.getTraceGoogleSignOption(this))
                .build();

        mAuthManager = TraceAuthenticationManager.getAuthenticationManager(this, mGoogleApiClient);
        mHandler = new MultipleCredentialsRequestHandler(mGoogleApiClient, mAuthManager);
        mListener = new LoginBroadcastListener(MainActivity.class, LoginActivity.class);
        registerReceiver(mListener, LoginBroadcastListener.getLoginIntentFilter());

        mAuthManager.login();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK){
            Log.e("E", "Failed with code " + requestCode);
            unregisterReceiver(mListener);
            Intent toLogin = new Intent(this, LoginActivity.class);
            startActivity(toLogin);
            finish();
        }else {
            mHandler.onRequestResult(requestCode, resultCode, data);
        }
    }


    @Override
    protected void onStop() {
        Log.e("E", "Stop");
        super.onStop();
        unregisterReceiver(mListener);
        finish();
    }
}
