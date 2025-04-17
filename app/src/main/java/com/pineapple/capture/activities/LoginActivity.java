package com.pineapple.capture.activities;

import android.content.Intent; //changing screen
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pineapple.capture.MainActivity;
import com.pineapple.capture.R;
import com.pineapple.capture.auth.AuthManager;
import com.pineapple.capture.databinding.ActivityLoginBinding;
import com.pineapple.capture.utils.NetworkUtils;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Capture_NoActionBar);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        authManager = AuthManager.getInstance();

        if (authManager.getCurrentUser() != null) {
            startMainActivity();
            finish();
            return;
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.loginButton.setOnClickListener(v -> handleLogin());
        binding.signupButton.setOnClickListener(v -> startSignupActivity());
        binding.resetPasswordLink.setOnClickListener(v -> handleResetPassword());
    }

    private void handleLogin() {
        if (!NetworkUtils.isConnected(LoginActivity.this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        authManager.login(email, password)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        startMainActivity();
                        finish();
                    } else {
                        String error = task.getException() != null ? 
                                     task.getException().getMessage() : 
                                     "Authentication failed";
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startSignupActivity() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    private void handleResetPassword() {
        String email = binding.emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        authManager.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() :
                                "Failed to send reset password email";
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();

                    }
                });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!show);
        binding.signupButton.setEnabled(!show);
        binding.emailEditText.setEnabled(!show);
        binding.passwordEditText.setEnabled(!show);
    }
} 