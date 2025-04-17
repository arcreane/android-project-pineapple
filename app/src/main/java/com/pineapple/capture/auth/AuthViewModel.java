package com.pineapple.capture.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pineapple.capture.profile.UserProfile;

public class AuthViewModel extends ViewModel {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private MutableLiveData<Boolean> authState;
    private MutableLiveData<String> errorMessage;

    public AuthViewModel() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        authState = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
    }

    public void signIn(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> authState.setValue(true))
            .addOnFailureListener(e -> errorMessage.setValue("Invalid username or password"));
    }

    public void signUp(String email, String password) {
        String username = email.replace("@pineapple.com", "");
        
        // Check if username already exists
        db.collection("usernames")
            .document(username)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    errorMessage.setValue("Username already taken");
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser user = authResult.getUser();
                            if (user != null) {
                                // user profile
                                UserProfile profile = new UserProfile(username, "");
                                db.collection("users")
                                    .document(user.getUid())
                                    .set(profile)
                                    .addOnSuccessListener(aVoid -> {
                                        // Reserve username
                                        db.collection("usernames")
                                            .document(username)
                                            .set(new UsernameReservation(user.getUid()))
                                            .addOnSuccessListener(aVoid2 -> authState.setValue(true))
                                            .addOnFailureListener(e -> errorMessage.setValue("Failed to create user profile"));
                                    })
                                    .addOnFailureListener(e -> errorMessage.setValue("Failed to create user profile"));
                            }
                        })
                        .addOnFailureListener(e -> errorMessage.setValue("Failed to create account"));
                }
            })
            .addOnFailureListener(e -> errorMessage.setValue("Failed to check username availability"));
    }

    public void resetPassword(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> authState.setValue(true))
                .addOnFailureListener(e -> errorMessage.setValue("Failed to send reset email"));
    }

    public LiveData<Boolean> getAuthState() {
        return authState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    private static class UsernameReservation {
        private String userId;
        
        public UsernameReservation() {}
        
        public UsernameReservation(String userId) {
            this.userId = userId;
        }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

    }
} 