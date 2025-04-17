package com.pineapple.capture.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pineapple.capture.MainActivity;
import com.pineapple.capture.R;


public class AuthActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private MaterialButton signupButton;
    private TextView errorText;
    private TextView resetPasswordLink;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);
        errorText = findViewById(R.id.error_text);
        resetPasswordLink = findViewById(R.id.reset_password_link);

        loginButton.setOnClickListener(v -> handleLogin());
        signupButton.setOnClickListener(v -> handleSignup());
        resetPasswordLink.setOnClickListener(v -> handleResetPassword());
        
        authViewModel.getAuthState().observe(this, isAuthenticated -> {
            if (isAuthenticated) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
        
        authViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                errorText.setText(error);
                errorText.setVisibility(View.VISIBLE);
            } else {
                errorText.setVisibility(View.GONE);
            }
        });

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
    
    private void handleLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        if (validateInput(username, password)) {
            authViewModel.signIn(username + "@pineapple.com", password);
        }
    }
    
    private void handleSignup() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        if (validateInput(username, password)) {
            authViewModel.signUp(username + "@pineapple.com", password);
        }
    }

    private void handleResetPassword() {
        String email = usernameInput.getText().toString().trim();

        if (email.isEmpty()) {
            errorText.setText("Please enter your email");
            errorText.setVisibility(View.VISIBLE);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText.setText("Invalid email format");
            errorText.setVisibility(View.VISIBLE);
        } else {
            authViewModel.resetPassword(email);
        }

    }

    
    private boolean validateInput(String username, String password) {
        if (username.isEmpty()) {
            errorText.setText("Username cannot be empty");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }
        
        if (password.isEmpty()) {
            errorText.setText("Password cannot be empty");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }
        
        if (password.length() < 6) {
            errorText.setText("Password must be at least 6 characters");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }
        
        errorText.setVisibility(View.GONE);
        return true;
    }

} 