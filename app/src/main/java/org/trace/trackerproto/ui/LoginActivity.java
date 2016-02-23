package org.trace.trackerproto.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.trace.trackerproto.Constants;
import org.trace.trackerproto.R;
import org.trace.trackerproto.store.TRACEStoreApiClient;

public class LoginActivity extends AppCompatActivity {

    private final String LOG_TAG = "LoginActivity";

    private Button loginBtn, cancelBtn;
    private EditText usernameForm, passwordForm;

    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginBtn = (Button) findViewById(R.id.loginBtn);
        cancelBtn= (Button) findViewById(R.id.cancelBtn);

        usernameForm = (EditText) findViewById(R.id.userIn);
        passwordForm = (EditText) findViewById(R.id.passwordIn);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameForm.getText().toString();
                String password = passwordForm.getText().toString();

                TRACEStoreApiClient.requestLogin(LoginActivity.this, username, password);
            }
        });


        // TRACEStore Service callbacks
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent.hasExtra(Constants.SUCCESS_LOGIN_KEY)
                        && intent.getBooleanExtra(Constants.SUCCESS_LOGIN_KEY, false)) {
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_LONG).show();
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
}
