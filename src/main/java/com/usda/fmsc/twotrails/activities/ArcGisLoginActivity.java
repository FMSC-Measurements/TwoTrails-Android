package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.core.io.UserCredentials;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.utilities.ArcGISTools;

public class ArcGisLoginActivity extends AppCompatActivity {
    public static String USERNAME = "Username";

    private EditText txtUsername;
    private EditText txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arc_gis_login);

        Bundle bundle = getIntent().getExtras();

        // Set up the login form.
        txtUsername = (EditText) findViewById(R.id.username);

        if (bundle.containsKey(USERNAME)) {
            txtUsername.setText(bundle.getString(USERNAME));
        }

        txtPassword = (EditText) findViewById(R.id.password);
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

        Button btnLogin = (Button) findViewById(R.id.sign_in_button);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });
        }
    }

    private void attemptLogin() {
        // Reset errors.
        txtUsername.setError(null);
        txtPassword.setError(null);

        // Store values at the time of the login attempt.
        String username = txtUsername.getText().toString();
        String password = txtPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password)) {
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

            if (!ArcGISTools.saveCredentials(ArcGisLoginActivity.this, credentials)) {
                Toast.makeText(ArcGisLoginActivity.this, "Unable to save Credentials", Toast.LENGTH_LONG).show();
            } else {
                setResult(RESULT_OK);
                finish();
            }
        }
    }
}

