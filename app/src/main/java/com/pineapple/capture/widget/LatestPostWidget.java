package com.pineapple.capture.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pineapple.capture.MainActivity;
import com.pineapple.capture.R;
import com.pineapple.capture.feed.FeedItem;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LatestPostWidget extends AppWidgetProvider {

    private static final String TAG = "LatestPostWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_latest_post);
        
        // Show loading state - hide post views, show empty view with loading text
        views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE);
        views.setTextViewText(R.id.widget_empty_view, "Loading...");
        
        // Set up click intent for the widget
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_post_image, pendingIntent);
        
        // Update the widget initially with loading state
        appWidgetManager.updateAppWidget(appWidgetId, views);
        
        // Fetch the latest post from Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        FeedItem latestPost = document.toObject(FeedItem.class);
                        latestPost.setId(document.getId());
                        
                        // Update widget with post data
                        updateWidgetWithPost(context, appWidgetManager, appWidgetId, latestPost);
                        return;
                    }
                } else {
                    // No posts found
                    views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE);
                    views.setTextViewText(R.id.widget_empty_view, "No posts available");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching latest post", e);
                views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE);
                views.setTextViewText(R.id.widget_empty_view, "Error loading post");
                appWidgetManager.updateAppWidget(appWidgetId, views);
            });
    }
    
    private void updateWidgetWithPost(Context context, AppWidgetManager appWidgetManager, 
                                    int appWidgetId, FeedItem post) {
        // Create a RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_latest_post);
        
        // Hide empty view
        views.setViewVisibility(R.id.widget_empty_view, View.GONE);
        
        // Set caption
        if (post.getContent() != null && !post.getContent().isEmpty()) {
            views.setTextViewText(R.id.widget_post_caption, post.getContent());
            views.setViewVisibility(R.id.widget_post_caption, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_post_caption, View.GONE);
        }
        
        // Set username
        views.setTextViewText(R.id.widget_username, post.getUsername() != null ? 
                post.getUsername() : "Anonymous");
        
        // Set up click intent to open the app
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("post_id", post.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_post_image, pendingIntent);
        
        // First update with what we have (without images)
        appWidgetManager.updateAppWidget(appWidgetId, views);
        
        // Load images in background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            try {
                // Load post image with Glide in background thread
                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    try {
                        Bitmap bitmap = Glide.with(context.getApplicationContext())
                                .asBitmap()
                                .load(post.getImageUrl())
                                .submit(400, 400)  // Limit size to prevent OOM
                                .get();
                        
                        views.setImageViewBitmap(R.id.widget_post_image, bitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading post image", e);
                    }
                }
                
                // Load user avatar with Glide in background thread
                if (post.getProfilePictureUrl() != null && !post.getProfilePictureUrl().isEmpty()) {
                    try {
                        Bitmap avatarBitmap = Glide.with(context.getApplicationContext())
                                .asBitmap()
                                .load(post.getProfilePictureUrl())
                                .circleCrop()
                                .submit(80, 80)  // Small size for avatar
                                .get();
                        
                        views.setImageViewBitmap(R.id.widget_user_avatar, avatarBitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading avatar image", e);
                    }
                }
                
                // Update widget with images on main thread
                handler.post(() -> {
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in background loading", e);
            }
        });
    }

    @Override
    public void onEnabled(Context context) {
        // Called when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Called when the last widget is disabled
        
        // Shutdown the executor service if needed
    }
} 