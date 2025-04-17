package com.pineapple.capture.feed;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedItem {
    private String id;
    private String userId;
    private String content;
    private String imageUrl;
    
    @ServerTimestamp
    private Timestamp timestamp;
    
    private int likes;
    private List<String> likedBy;
    private List<Map<String, Object>> comments;
    private String profilePictureUrl;
    private String username;
    
    @Exclude
    private boolean isLikedByCurrentUser;

    public FeedItem() {
        this.likes = 0;
        this.likedBy = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.isLikedByCurrentUser = false;
    }

    public FeedItem(String userId, String content, String imageUrl, String profilePictureUrl, String username) {
        this.userId = userId;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = Timestamp.now();
        this.likes = 0;
        this.likedBy = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.profilePictureUrl = profilePictureUrl;
        this.username = username;
        this.isLikedByCurrentUser = false;
    }

    // Convert to Firestore Map for easier saving
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("content", content);
        map.put("imageUrl", imageUrl);
        map.put("timestamp", Timestamp.now()); // Use current timestamp as server timestamp may not be applied yet
        map.put("likes", likes);
        map.put("likedBy", likedBy);
        map.put("comments", comments);
        map.put("profilePictureUrl", profilePictureUrl);
        map.put("username", username);
        return map;
    }

    public void addComment(String userId, String username, String text) {
        if (comments == null) {
            comments = new ArrayList<>();
        }
        
        Map<String, Object> comment = new HashMap<>();
        comment.put("userId", userId);
        comment.put("username", username);
        comment.put("text", text);
        comment.put("timestamp", Timestamp.now());
        
        comments.add(comment);
    }
    
    public boolean toggleLike(String userId) {
        if (likedBy == null) {
            likedBy = new ArrayList<>();
        }
        
        if (likedBy.contains(userId)) {
            // User already liked, so unlike
            likedBy.remove(userId);
            likes = Math.max(0, likes - 1);
            isLikedByCurrentUser = false;
            return false;
        } else {
            likedBy.add(userId);
            likes += 1;
            isLikedByCurrentUser = true;
            return true;
        }
    }
    
    public boolean isLikedBy(String userId) {
        return likedBy != null && likedBy.contains(userId);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public Timestamp getTimestamp() { 
        if (timestamp == null) {
            timestamp = Timestamp.now();
        }
        return timestamp; 
    }
    
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    
    public Date getTimestampAsDate() {
        if (timestamp == null) {
            return new Date();
        }
        return timestamp.toDate();
    }
    
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public List<String> getLikedBy() { return likedBy; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }
    
    public List<Map<String, Object>> getComments() { return comments; }
    public void setComments(List<Map<String, Object>> comments) { this.comments = comments; }
    
    @Exclude
    public boolean isLikedByCurrentUser() { return isLikedByCurrentUser; }
    
    @Exclude
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.isLikedByCurrentUser = likedByCurrentUser; }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }
    
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    @Override
    public String toString() {
        return "FeedItem{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", content='" + content + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", timestamp=" + timestamp +
                ", likes=" + likes +
                ", comments=" + (comments != null ? comments.size() : 0) +
                ", username='" + username + '\'' +
                '}';
    }
}