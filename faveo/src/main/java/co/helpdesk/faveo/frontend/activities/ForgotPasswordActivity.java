package co.helpdesk.faveo.frontend.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import co.helpdesk.faveo.R;
import co.helpdesk.faveo.frontend.receivers.NetworkUtil;


public class ForgotPasswordActivity extends AppCompatActivity {

    Snackbar networksnackbar;
    TextView textViewErrorEmail;
    EditText editTextEmail;
    Button buttonResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        setUpViews();
        buttonResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void setUpViews() {
        textViewErrorEmail = (TextView) findViewById(R.id.textView_error_email);
        editTextEmail = (EditText) findViewById(R.id.editText_email);
        buttonResetPassword = (Button) findViewById(R.id.button_reset_password);
        networksnackbar = Snackbar.make(findViewById(android.R.id.content), "No internet connection!", Snackbar.LENGTH_LONG);
        networksnackbar.setAction("SETTINGS", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        networksnackbar.setActionTextColor(getResources().getColor(R.color.blue_300));
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (NetworkUtil.getConnectivityStatus(this) == 0) {
            if (networksnackbar != null)
                networksnackbar.show();
        }

    }
}
