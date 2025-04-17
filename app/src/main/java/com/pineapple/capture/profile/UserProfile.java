package com.pineapple.capture.profile;

public class UserProfile {
    private String name;
    private String bio;
    private String profileImageUrl;

    // Required empty constructor for Firestore
    public UserProfile() {}

    public UserProfile(String name, String bio) {
        this.name = name;
        this.bio = bio;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
} 