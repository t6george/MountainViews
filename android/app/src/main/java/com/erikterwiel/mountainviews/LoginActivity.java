package com.erikterwiel.mountainviews;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity.java";

    private EditText mUsername;
    private EditText mPassword;
    private Button mLogin;
    private Button mRegister;
    private Button mForgot;

    private CognitoUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate() called");
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ActivityCompat.requestPermissions(
                this,
                new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                100);

        mUsername = (EditText) findViewById(R.id.login_user);
        mPassword = (EditText) findViewById(R.id.login_password);
        mLogin = (Button) findViewById(R.id.login_in);
        mRegister = (Button) findViewById(R.id.login_up);

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        CognitoUserPool userPool = new CognitoUserPool(this, Constants.cognitoPoolID,
                Constants.cognitoClientID, Constants.cognitoClientSecret, clientConfiguration);
        mUser = userPool.getUser();

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new LoginAttempt().execute();
            }
        });

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(registerIntent);
            }
        });
    }

    private class LoginAttempt extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            mDialog = ProgressDialog.show(LoginActivity.this,
                    getString(R.string.login_dialog),
                    getString(R.string.login_wait));
        }

        @Override
        protected Void doInBackground(Void... inputs) {
            AuthenticationHandler handler = new AuthenticationHandler() {
                @Override
                public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
                    Toast.makeText(LoginActivity.this,
                            "Sign in success.", Toast.LENGTH_LONG).show();
                    Intent homeIntent = new Intent(LoginActivity.this, HomeActivity.class);
                    homeIntent.putExtra("username", mUsername.getText().toString());
                    startActivity(homeIntent);
                }

                @Override
                public void getAuthenticationDetails(
                        AuthenticationContinuation authenticationContinuation, String userId) {
                    AuthenticationDetails details = new AuthenticationDetails(
                            mUsername.getText().toString(),
                            mPassword.getText().toString(), null);
                    authenticationContinuation.setAuthenticationDetails(details);
                    authenticationContinuation.continueTask();
                }

                @Override
                public void getMFACode(MultiFactorAuthenticationContinuation continuation) {
                    continuation.setMfaCode(null);
                    continuation.continueTask();
                }

                @Override
                public void authenticationChallenge(ChallengeContinuation continuation) {}

                @Override
                public void onFailure(Exception exception) {
                    Toast.makeText(LoginActivity.this, "Sign in failed, try again.",
                            Toast.LENGTH_LONG).show();
                    exception.printStackTrace();
                }
            };
            mUser.getSessionInBackground(handler);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mDialog.dismiss();
        }
    }
}
