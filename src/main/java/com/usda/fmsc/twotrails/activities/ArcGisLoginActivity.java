package com.usda.fmsc.twotrails.activities;

import android.Manifest;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.core.io.UserCredentials;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.Consts;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;

public class ArcGisLoginActivity extends CustomToolbarActivity {
    public static String USERNAME = "Username";

    private EditText txtUsername;
    private EditText txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arc_gis_login);

        Bundle bundle = getIntent().getExtras();

        // Set up the login form.
        txtUsername = findViewById(R.id.username);

        if (bundle != null && bundle.containsKey(USERNAME)) {
            txtUsername.setText(bundle.getString(USERNAME));
        }

        txtPassword = findViewById(R.id.password);
        txtPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button btnLogin = findViewById(R.id.sign_in_button);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestPhoneState() {
        AndroidUtils.App.requestPhonePermission(ArcGisLoginActivity.this, Consts.Codes.Requests.PHONE,
                "In order to securely save your credentials we need to use your phone's ID.");
    }

    private void attemptLogin() {
        if (AndroidUtils.App.checkPermission(ArcGisLoginActivity.this, Manifest.permission.READ_PHONE_STATE)) {
            // Reset errors.
            txtUsername.setError(null);
            txtPassword.setError(null);

            // Store values at the time of the login attempt.
            String username = txtUsername.getText().toString();
            String password = txtPassword.getText().toString();

            boolean cancel = false;
            View focusView = null;

            // Check for a valid password, if the user entered one.
            if (TextUtils.isEmpty(password)) {
                txtPassword.setError(getString(R.string.error_invalid_password));
                focusView = txtPassword;
                cancel = true;
            }

            if (TextUtils.isEmpty(username)) {
                txtUsername.setError(getString(R.string.error_field_required));
                focusView = txtUsername;
                cancel = true;
            }

            if (cancel) {
                focusView.requestFocus();
            } else {
                UserCredentials credentials = new UserCredentials();
                credentials.setUserAccount(username, password);

                //todo check to see if credentials are valid

                if (!TtAppCtx.getArcGISTools().saveCredentials(ArcGisLoginActivity.this, credentials)) {
                    Toast.makeText(ArcGisLoginActivity.this, "Unable to save Credentials", Toast.LENGTH_LONG).show();
                } else {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        } else {
            requestPhoneState();
        }
    }
}

