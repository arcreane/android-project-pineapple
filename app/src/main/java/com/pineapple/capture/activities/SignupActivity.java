package com.pineapple.capture.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.pineapple.capture.MainActivity;
import com.pineapple.capture.auth.AuthManager;
import com.pineapple.capture.databinding.ActivitySignupBinding;
import com.pineapple.capture.utils.NetworkUtils;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance();

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.signupButton.setOnClickListener(v -> handleSignup());
        binding.loginButton.setOnClickListener(v -> finish());
    }

    private void handleSignup() {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = binding.displayNameEditText.getText().toString().trim();
        String username = binding.usernameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (displayName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Username validation
        if (username.length() < 1 || username.length() > 30) {
            Toast.makeText(this, "Username must be 1-30 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.contains(" ")) {
            Toast.makeText(this, "Username cannot contain spaces", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!username.matches("^[a-zA-Z0-9._]+$")) {
            Toast.makeText(this, "Username can only contain letters, numbers, periods, and underscores", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.startsWith(".") || username.endsWith(".")) {
            Toast.makeText(this, "Username cannot start or end with a period", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        authManager.isUsernameUnique(username, isUnique -> {
            if (!isUnique) {
                showLoading(false);
                Toast.makeText(this, "Username is already taken", Toast.LENGTH_SHORT).show();
                return;
            }
            authManager.signup(displayName, username, email, password)
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            startMainActivity();
                        } else {
                            String error = task.getException() != null ?
                                    task.getException().getMessage() :
                                    "Signup failed";
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.signupButton.setEnabled(!show);
        binding.loginButton.setEnabled(!show);
        binding.displayNameEditText.setEnabled(!show);
        binding.emailEditText.setEnabled(!show);
        binding.passwordEditText.setEnabled(!show);
    }
}
