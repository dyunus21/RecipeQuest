package com.example.recipeapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.recipeapp.databinding.ActivityLoginBinding;
import com.parse.ParseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.setActivityLoginController(this);

        if (ParseUser.getCurrentUser() != null) {
            goMainActivity();
        }
    }

    // TODO: Handle errors
    public void loginUser() {
        Log.i(TAG, "Attempting to login user");
        final String username = binding.etUsername.getText().toString();
        final String password = binding.etPassword.getText().toString();
        ParseUser.logInInBackground(username, password, (user, e) -> {
            if (e != null) {
                Log.e(TAG, "Error with login", e);
                return;
            }

            goMainActivity();
            Toast.makeText(LoginActivity.this, "Welcome " + ParseUser.getCurrentUser().getUsername(), Toast.LENGTH_SHORT).show();
        });
    }

    private void goMainActivity() {
        final Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void goRegister() {
        final Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }
}
