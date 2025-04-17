package com.pineapple.capture.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.ArrayList;

public class MainFeedViewModel extends ViewModel {
    private FirebaseFirestore db;
    private MutableLiveData<List<FeedItem>> feedItems;

    public MainFeedViewModel() {
        db = FirebaseFirestore.getInstance();
        feedItems = new MutableLiveData<>(new ArrayList<>());
        loadFeedItems();
    }

    private void loadFeedItems() {
        db.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    return;
                }
                
                List<FeedItem> items = new ArrayList<>();
                if (value != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value) {
                        FeedItem item = doc.toObject(FeedItem.class);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                }
                feedItems.setValue(items);
            });
    }

    public LiveData<List<FeedItem>> getFeedItems() {
        return feedItems;
    }
} 