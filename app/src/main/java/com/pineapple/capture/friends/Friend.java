package com.pineapple.capture.friends;

public class Friend {
    private String userId;
    private String name;
    private String profileImageUrl;
    private long friendsSince;

    //empty constructor for Firestore
    public Friend() {}

    public Friend(String userId, String name, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.friendsSince = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public long getFriendsSince() { return friendsSince; }
    public void setFriendsSince(long friendsSince) { this.friendsSince = friendsSince; }
} 