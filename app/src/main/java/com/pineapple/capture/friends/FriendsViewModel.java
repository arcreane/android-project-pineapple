package com.pineapple.capture.friends;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.ArrayList;

public class FriendsViewModel extends ViewModel {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MutableLiveData<List<Friend>> friends;

    public FriendsViewModel() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        friends = new MutableLiveData<>(new ArrayList<>());
        loadFriends();
    }

    private void loadFriends() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId != null) {
            db.collection("users").document(userId)
                .collection("friends")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    List<Friend> friendsList = new ArrayList<>();
                    if (value != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value) {
                            Friend friend = doc.toObject(Friend.class);
                            if (friend != null) {
                                friendsList.add(friend);
                            }
                        }
                    }
                    friends.setValue(friendsList);
                });
        }
    }

    public void addFriend(String friendId) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId != null) {
            db.collection("users").document(friendId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Friend friend = documentSnapshot.toObject(Friend.class);
                        if (friend != null) {
                            db.collection("users").document(userId)
                                .collection("friends")
                                .document(friendId)
                                .set(friend);
                        }
                    }
                });
        }
    }

    public LiveData<List<Friend>> getFriends() {
        return friends;
    }
} 