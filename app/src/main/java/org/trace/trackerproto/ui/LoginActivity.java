package org.trace.trackerproto.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.trace.storeclient.TraceAuthenticationManager;
import org.trace.storeclient.auth.MultipleCredentialsRequestHandler;
import org.trace.storeclient.exceptions.NetworkConnectivityRequiredException;
import org.trace.tracker.TrackingConstants;
import org.trace.trackerproto.R;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Login";
    private final String LOG_TAG = "LoginActivity";

    private BroadcastReceiver mReceiver;

    private TraceAuthenticationManager mAuthManager;
    private MultipleCredentialsRequestHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // TRACEStore Service callbacks
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent.hasExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA)
                        && intent.getBooleanExtra(TrackingConstants.store.SUCCESS_LOGIN_EXTRA, false)) {
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_LONG).show();

                    Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(mainActivity);
                    finish();

                }else{

                    String error;
                    if(intent.hasExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA))
                        error = intent.getStringExtra(TrackingConstants.store.LOGIN_ERROR_MSG_EXTRA);
                    else
                        error = "Login failed!";

                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(TrackingConstants.store.LOGIN_ACTION);
        registerReceiver(mReceiver, filter);

        setupGoogleSignin();
        setupTRACENativeSignin();


        mAuthManager = TraceAuthenticationManager.getAuthenticationManager(this, mGoogleApiClient);
        mHandler = new MultipleCredentialsRequestHandler(mGoogleApiClient, mAuthManager);
        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);


        setupCredentialRemoval();
    }

    @Override
    public void finish() {
        unregisterReceiver(mReceiver);
        super.finish();
    }



    /* Devices Management
    /* Devices Management
    /* Devices Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private ConnectivityManager mConnectivityManager;

    private boolean isNetworkConnected(){
        return mConnectivityManager.getActiveNetworkInfo() != null;
    }


    private void buildAlertMessageNoConnectivity() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.network_enable_login_rationale))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    /* TRACE Native Sign-in
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private Button loginBtn, cancelBtn;
    private EditText usernameForm, passwordForm;

    private void setupTRACENativeSignin(){

        loginBtn = (Button) findViewById(R.id.loginBtn);
        cancelBtn= (Button) findViewById(R.id.cancelBtn);

        usernameForm = (EditText) findViewById(R.id.userIn);
        passwordForm = (EditText) findViewById(R.id.passwordIn);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isNetworkConnected()) {

                    String username = usernameForm.getText().toString();
                    String password = passwordForm.getText().toString();

                    //TRACEStore.Client.requestLogin(LoginActivity.this, username, password);

                    mAuthManager.login(username, password);

                }else
                    buildAlertMessageNoConnectivity();

            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buildAlertMessageCancel();
            }
        });
    }

    private void buildAlertMessageCancel() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.exit_no_login_rationale))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                        homeIntent.addCategory( Intent.CATEGORY_HOME );
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(homeIntent);
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    /* Google SignIn
    /* Google SignIn
    /* Google SignIn
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */



    private SignInButton googleSignIn;

    private GoogleApiClient mGoogleApiClient;

    private void setupGoogleSignin(){

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.trace_client_id))
                .requestEmail()
                .build();

        googleSignIn = (SignInButton) findViewById(R.id.sign_in_button);
        googleSignIn.setSize(SignInButton.SIZE_STANDARD);
        googleSignIn.setScopes(gso.getScopeArray());

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        googleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void signIn(){
        if(!isNetworkConnected()) {
            buildAlertMessageNoConnectivity();
        }else {
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, TraceAuthenticationManager.RC_SIGN_IN);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Connection failed because " + connectionResult.getErrorMessage());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        mHandler.onRequestResult(requestCode, resultCode, data);

        /*
        if(resultCode != RESULT_OK) {
            Log.e(TAG, "Failed at code " + requestCode);
            return;
        }


        switch (requestCode){
            case TraceAuthenticationManager.RC_SIGN_IN:
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                Log.d(TAG, "onActivityResult:GET_TOKEN:success:" + result.getStatus().isSuccess());

                if (result.isSuccess()) {
                    GoogleSignInAccount acct = result.getSignInAccount();
                    mAuthManager.login(acct, TraceAuthenticationManager.GrantType.google);
                }
                break;
            case TraceAuthenticationManager.RC_LOAD:
                Log.d(LOG_TAG, "Read");
                if (resultCode == RESULT_OK) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    //onCredentialRetrieved(credential);
                } else {
                    Log.e(TAG, "Credential Read: NOT OK");
                    Toast.makeText(this, "Credential Read Failed", Toast.LENGTH_SHORT).show();
                }
                break;
            case TraceAuthenticationManager.RC_SAVE:
                Log.d(LOG_TAG, "Save");
                break;
            case TraceAuthenticationManager.RC_DELETE:
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                mAuthManager.removeCredential(credential);
                break;
            default:
                Log.e(LOG_TAG, "Unknown request code "+requestCode);
        }
        */
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            //findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            //mStatusTextView.setText(R.string.signed_out);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            //findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }


    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        Log.d(TAG, "handleSignInResult:" + result.toString());
        Log.d(TAG, "handleSignInResult:" + result.getStatus());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            //updateUI(true);
            Toast.makeText(this, acct.getDisplayName(), Toast.LENGTH_LONG).show();

        } else {
            // Signed out, show unauthenticated UI.
            //updateUI(false);
            signOut();
        }
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d(TAG, "signOut:onResult:" + status);
                        updateUI(false);
                    }
                });
    }

    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        Log.d(TAG, "revokeAccess:onResult:" + status);
                        updateUI(false);
                    }
                });
    }

    /*
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private Button clearCredentialsBtn;

    public void setupCredentialRemoval(){
        clearCredentialsBtn = (Button) findViewById(R.id.clear_credentials_btn);
        clearCredentialsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mAuthManager.removeAllStoredCredentials();
                } catch (NetworkConnectivityRequiredException e) {
                    buildAlertMessageNoConnectivity();
                }
            }
        });
    }
}
