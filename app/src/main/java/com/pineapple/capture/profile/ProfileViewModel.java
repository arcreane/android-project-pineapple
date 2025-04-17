package com.pineapple.capture.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pineapple.capture.models.User;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final MutableLiveData<User> userData;
    private final MutableLiveData<String> errorMessage;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<Boolean> deletionResult;

    public ProfileViewModel() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userData = new MutableLiveData<>();
        errorMessage = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        deletionResult = new MutableLiveData<>();
        loadUserData();
    }

    public void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            isLoading.setValue(true);
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            errorMessage.setValue("User document does not exist");
                            isLoading.setValue(false);
                            return;
                        }
                        
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setId(currentUser.getUid());
                            // Set display name from Firebase User if not set in Firestore
                            if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
                                user.setDisplayName(currentUser.getDisplayName());
                            }
                            
                            // Fetch post count from Firestore
                            fetchPostCount(user);
                        } else {
                            errorMessage.setValue("Failed to convert document to User object");
                            isLoading.setValue(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        errorMessage.setValue("Failed to load user data: " + e.getMessage());
                        isLoading.setValue(false);
                    });
        } else {
            errorMessage.setValue("No current user found");
        }
    }
    
    private void fetchPostCount(User user) {
        // Query posts collection for documents with matching userId
        db.collection("posts")
            .whereEqualTo("userId", user.getId())
            .get()
            .addOnSuccessListener(querySnapshot -> {
                int count = querySnapshot.size();
                user.setPostCount(count);
                
                // Update UI with the complete user data including post count
                userData.setValue(user);
                isLoading.setValue(false);
            })
            .addOnFailureListener(e -> {
                // Still update UI with user data, just without accurate post count
                userData.setValue(user);
                errorMessage.setValue("Failed to load post count: " + e.getMessage());
                isLoading.setValue(false);
            });
    }

    public void updateEmail(String currentPassword, String newEmail) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
                        return;
        }

        // First, re-authenticate the user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // After successful re-authentication, update the email
                    user.updateEmail(newEmail)
                        .addOnCompleteListener(emailTask -> {
                            if (emailTask.isSuccessful()) {
                                // Update email in Firestore
                                db.collection("users").document(user.getUid())
                                    .update("email", newEmail)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update the local user data
                                        User currentUser = userData.getValue();
                                        if (currentUser != null) {
                                            currentUser.setEmail(newEmail);
                                            userData.setValue(currentUser);
                                        }
                                        errorMessage.setValue("Email updated successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        // If Firestore update fails, revert the email in Firebase Auth
                                        user.updateEmail(user.getEmail())
                                            .addOnCompleteListener(revertTask -> {
                                                if (!revertTask.isSuccessful()) {
                                                    errorMessage.setValue("Failed to update email and couldn't revert changes. Please contact support.");
                                                } else {
                                                    errorMessage.setValue("Failed to update email in database. Changes reverted.");
                                                }
                                            });
                                    });
                            } else {
                                Exception exception = emailTask.getException();
                                if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
                                    errorMessage.setValue("Please sign in again to change your email");
                                } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                    errorMessage.setValue("Invalid email format");
                                } else {
                                    errorMessage.setValue("Failed to update email: " + exception.getMessage());
                                }
                            }
                        });
                } else {
                    Exception exception = task.getException();
                    if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                        errorMessage.setValue("Current password is incorrect");
                    } else if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
                        errorMessage.setValue("Please sign in again to change your email");
                    } else {
                        errorMessage.setValue("Authentication failed: " + exception.getMessage());
                    }
                }
            });
    }

    public void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            isLoading.setValue(true);
            // Re-authenticate user before changing password
            com.google.firebase.auth.AuthCredential credential = 
                com.google.firebase.auth.EmailAuthProvider.getCredential(
                    user.getEmail(), currentPassword);
            
            user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.updatePassword(newPassword)
                            .addOnCompleteListener(passwordTask -> {
                                if (passwordTask.isSuccessful()) {
                                    errorMessage.setValue("Password updated successfully");
                                } else {
                                    errorMessage.setValue("Failed to update password: " + 
                                        passwordTask.getException().getMessage());
                                }
                                isLoading.setValue(false);
                            });
                    } else {
                        errorMessage.setValue("Current password is incorrect");
                        isLoading.setValue(false);
                    }
                });
        }
    }

    public void deleteAccount(String currentPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            isLoading.setValue(true);
            // Re-authenticate user before deleting account
            com.google.firebase.auth.AuthCredential credential = 
                com.google.firebase.auth.EmailAuthProvider.getCredential(
                    user.getEmail(), currentPassword);
            
            user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Delete user data from Firestore
                        db.collection("users").document(user.getUid())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Delete user account
                                user.delete()
                                    .addOnCompleteListener(deleteTask -> {
                                        if (deleteTask.isSuccessful()) {
                                            deletionResult.setValue(true);
                                            errorMessage.setValue("Account deleted successfully");
                                        } else {
                                            deletionResult.setValue(false);
                                            errorMessage.setValue("Failed to delete account: " + 
                                                deleteTask.getException().getMessage());
                                        }
                                        isLoading.setValue(false);
                                    });
                            })
                            .addOnFailureListener(e -> {
                                deletionResult.setValue(false);
                                errorMessage.setValue("Failed to delete user data");
                                isLoading.setValue(false);
                            });
                    } else {
                        deletionResult.setValue(false);
                        errorMessage.setValue("Current password is incorrect");
                        isLoading.setValue(false);
                    }
                });
        }
    }

    public void updateDisplayName(String newUsername) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }

        // Validate username format
        if (!isValidUsername(newUsername)) {
            errorMessage.setValue("Invalid username format");
            return;
        }

        // First check if the new username is the same as the current one
        User currentUser = userData.getValue();
        if (currentUser != null && currentUser.getUsername().equals(newUsername)) {
            errorMessage.setValue("This is already your username");
            return;
        }

        // Update username in Firestore
        db.collection("users").document(user.getUid())
            .update("username", newUsername)
            .addOnSuccessListener(aVoid -> {
                // Update the local user data
                if (currentUser != null) {
                    currentUser.setUsername(newUsername);
                    userData.setValue(currentUser);
                }
                errorMessage.setValue("Username updated successfully");
            })
            .addOnFailureListener(e -> {
                if (e.getMessage().contains("PERMISSION_DENIED")) {
                    errorMessage.setValue("Permission denied. Please check your Firestore security rules.");
                } else {
                    errorMessage.setValue("Failed to update username: " + e.getMessage());
                }
            });
    }

    public void updateDisplayNameOnly(String newDisplayName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }

        // Update display name in Firestore
        db.collection("users").document(user.getUid())
            .update("displayName", newDisplayName)
            .addOnSuccessListener(aVoid -> {
                // Update the local user data
                User currentUser = userData.getValue();
                if (currentUser != null) {
                    currentUser.setDisplayName(newDisplayName);
                    userData.setValue(currentUser);
                }
                errorMessage.setValue("Display name updated successfully");
            })
            .addOnFailureListener(e -> {
                errorMessage.setValue("Failed to update display name: " + e.getMessage());
            });
    }

    public void updateBio(String newBio) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }

        // Validate bio length
        if (newBio.length() > 150) {
            errorMessage.setValue("Bio cannot be longer than 150 characters");
            return;
        }

        // Update bio in Firestore
        db.collection("users").document(user.getUid())
            .update("bio", newBio)
            .addOnSuccessListener(aVoid -> {
                // Update the local user data
                User currentUser = userData.getValue();
                if (currentUser != null) {
                    currentUser.setBio(newBio);
                    userData.setValue(currentUser);
                }
                errorMessage.setValue("Bio updated successfully");
            })
            .addOnFailureListener(e -> {
                errorMessage.setValue("Failed to update bio: " + e.getMessage());
            });
    }

    public void updateLocation(String newLocation) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }

        // Validate location length
        if (newLocation.length() > 50) {
            errorMessage.setValue("Location cannot be longer than 50 characters");
            return;
        }

        // Update location in Firestore
        db.collection("users").document(user.getUid())
            .update("location", newLocation)
            .addOnSuccessListener(aVoid -> {
                // Update the local user data
                User currentUser = userData.getValue();
                if (currentUser != null) {
                    currentUser.setLocation(newLocation);
                    userData.setValue(currentUser);
                }
                errorMessage.setValue("Location updated successfully");
            })
            .addOnFailureListener(e -> {
                errorMessage.setValue("Failed to update location: " + e.getMessage());
            });
    }

    public void updateInterests(List<String> interests) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }

        // Update interests in Firestore
        db.collection("users").document(user.getUid())
            .update("interests", interests)
            .addOnSuccessListener(aVoid -> {
                // Update the local user data
                User currentUser = userData.getValue();
                if (currentUser != null) {
                    currentUser.setInterests(interests);
                    userData.setValue(currentUser);
                }
                errorMessage.setValue("Interests updated successfully");
            })
            .addOnFailureListener(e -> {
                errorMessage.setValue("Failed to update interests: " + e.getMessage());
            });
    }

    public void followUser(String userIdToFollow) {
        FirebaseUser currentAuthUser = mAuth.getCurrentUser();
        if (currentAuthUser == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }
        
        String currentUserId = currentAuthUser.getUid();
        
        // Don't allow following yourself
        if (currentUserId.equals(userIdToFollow)) {
            errorMessage.setValue("You cannot follow yourself");
            return;
        }
        
        // Get a reference to the current user's document
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(currentUserDoc -> {
                User currentUser = currentUserDoc.toObject(User.class);
                if (currentUser == null) {
                    errorMessage.setValue("Could not load current user data");
                    return;
                }
                
                // Check if already following
                List<String> following = currentUser.getFollowing();
                if (following != null && following.contains(userIdToFollow)) {
                    errorMessage.setValue("You are already following this user");
                    return;
                }
                
                // Add to current user's following list
                if (following == null) {
                    following = new ArrayList<>();
                }
                following.add(userIdToFollow);
                
                // Create a final copy of the following list for the transaction
                final List<String> finalFollowing = new ArrayList<>(following);
                
                // Update Firestore in a transaction
                db.runTransaction(transaction -> {
                    // Update current user's following list
                    transaction.update(db.collection("users").document(currentUserId), 
                            "following", finalFollowing);
                    
                    // Update target user's followers list
                    db.collection("users").document(userIdToFollow)
                        .get()
                        .addOnSuccessListener(targetUserDoc -> {
                            User targetUser = targetUserDoc.toObject(User.class);
                            if (targetUser != null) {
                                List<String> followers = targetUser.getFollowers();
                                if (followers == null) {
                                    followers = new ArrayList<>();
                                }
                                followers.add(currentUserId);
                                db.collection("users").document(userIdToFollow)
                                    .update("followers", followers);
                            }
                        });
                    
                    return null;
                }).addOnSuccessListener(aVoid -> {
                    // Update local user data
                    User updatedUser = userData.getValue();
                    if (updatedUser != null) {
                        updatedUser.setFollowing(finalFollowing);
                        userData.setValue(updatedUser);
                    }
                    errorMessage.setValue("Now following user");
                    loadUserData(); // Refresh to get updated counts
                }).addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to follow user: " + e.getMessage());
                });
            })
            .addOnFailureListener(e -> {
                errorMessage.setValue("Error loading user data: " + e.getMessage());
            });
    }
    
    public void unfollowUser(String userIdToUnfollow) {
        FirebaseUser currentAuthUser = mAuth.getCurrentUser();
        if (currentAuthUser == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }
        
        String currentUserId = currentAuthUser.getUid();
        
        // Get a reference to the current user's document
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(currentUserDoc -> {
                User currentUser = currentUserDoc.toObject(User.class);
                if (currentUser == null) {
                    errorMessage.setValue("Could not load current user data");
                    return;
                }
                
                // Check if actually following
                List<String> following = currentUser.getFollowing();
                if (following == null || !following.contains(userIdToUnfollow)) {
                    errorMessage.setValue("You are not following this user");
                    return;
                }
                
                // Remove from current user's following list
                following.remove(userIdToUnfollow);
                
                // Create a final copy of the following list for the transaction
                final List<String> finalFollowing = new ArrayList<>(following);
                
                // Update Firestore in a transaction
                db.runTransaction(transaction -> {
                    // Update current user's following list
                    transaction.update(db.collection("users").document(currentUserId), 
                            "following", finalFollowing);
                    
                    // Update target user's followers list
                    db.collection("users").document(userIdToUnfollow)
                        .get()
                        .addOnSuccessListener(targetUserDoc -> {
                            User targetUser = targetUserDoc.toObject(User.class);
                            if (targetUser != null) {
                                List<String> followers = targetUser.getFollowers();
                                if (followers != null) {
                                    followers.remove(currentUserId);
                                    db.collection("users").document(userIdToUnfollow)
                                        .update("followers", followers);
                                }
                            }
                        });
                    
                    return null;
                }).addOnSuccessListener(aVoid -> {
                    // Update local user data
                    User updatedUser = userData.getValue();
                    if (updatedUser != null) {
                        updatedUser.setFollowing(finalFollowing);
                        userData.setValue(updatedUser);
                    }
                    errorMessage.setValue("Unfollowed user");
                    loadUserData(); // Refresh to get updated counts
                }).addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to unfollow user: " + e.getMessage());
                });
            })
            .addOnFailureListener(e -> {
                errorMessage.setValue("Error loading user data: " + e.getMessage());
            });
    }

    private boolean isValidUsername(String username) {
        // Minimum 3 characters, maximum 30 characters
        // Only alphanumeric characters, underscores, and periods
        return username != null && username.matches("^[a-zA-Z0-9_.]{3,30}$");
    }

    public void updateProfilePicture(String imageUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("No user is currently signed in");
            return;
        }

        isLoading.setValue(true);
        
        // Create a list with the profile image URL
        List<String> profileUrls = new ArrayList<>();
        profileUrls.add(imageUrl);
        
        // Update profile picture in Firestore
        db.collection("users").document(user.getUid())
            .update("profilePictureUrl", profileUrls)
            .addOnSuccessListener(aVoid -> {
                // Update the local user data
                User currentUser = userData.getValue();
                if (currentUser != null) {
                    currentUser.setProfilePictureUrl(profileUrls);
                    userData.setValue(currentUser);
                }
                errorMessage.setValue("Profile picture updated successfully");
                isLoading.setValue(false);
            })
            .addOnFailureListener(e -> {
                errorMessage.setValue("Failed to update profile picture: " + e.getMessage());
                isLoading.setValue(false);
            });
    }

    public LiveData<User> getUserData() {
        return userData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getDeletionResult() {
        return deletionResult;
    }
} 