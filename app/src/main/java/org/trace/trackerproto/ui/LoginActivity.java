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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.trace.tracking.Constants;
import org.trace.trackerproto.R;
import org.trace.tracking.store.TRACEStoreApiClient;

public class LoginActivity extends AppCompatActivity {

    private final String LOG_TAG = "LoginActivity";

    private Button loginBtn, cancelBtn;
    private EditText usernameForm, passwordForm;

    private BroadcastReceiver mReceiver;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

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

                    TRACEStoreApiClient.requestLogin(LoginActivity.this, username, password);
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


        // TRACEStore Service callbacks
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent.hasExtra(Constants.SUCCESS_LOGIN_KEY)
                        && intent.getBooleanExtra(Constants.SUCCESS_LOGIN_KEY, false)) {
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_LONG).show();

                    Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(mainActivity);
                    finish();

                }else
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_LONG).show();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.LOGIN_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void finish() {
        unregisterReceiver(mReceiver);
        super.finish();
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


}
