package com.pineapple.capture.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.pineapple.capture.R;
import com.pineapple.capture.databinding.FragmentHomeBinding;
import com.pineapple.capture.feed.FeedItem;
import com.pineapple.capture.profile.ProfileViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements OnDeleteClickListener {
    private FragmentHomeBinding binding;
    private ProfileViewModel profileViewModel;
    private RecyclerView feedRecyclerView;
    private FeedAdapter feedAdapter;
    private List<FeedItem> feedItems;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        feedRecyclerView = root.findViewById(R.id.feed_recycler_view);
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout);
        emptyStateLayout = root.findViewById(R.id.empty_state_layout);

        feedItems = new ArrayList<>();
        
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        feedAdapter = new FeedAdapter(feedItems, this);
        feedAdapter.setHasStableIds(true); // Improve RecyclerView performance
        feedRecyclerView.setAdapter(feedAdapter);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        swipeRefreshLayout.setOnRefreshListener(this::loadFeedPosts);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_blue);
        
        loadFeedPosts();
        
        return root;
    }

    @Override
    public void onDeleteClick(String postId) {
        deletePost(postId); // Gọi hàm xoá
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d("HomeFragment", "onResume - reloading posts");
        loadFeedPosts();
    }

    public void loadFeedPosts() {
        if (getActivity() == null) return;
        
        Log.d("HomeFragment", "Loading feed posts...");
        swipeRefreshLayout.setRefreshing(true);

        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        db.collection("posts").get()
            .addOnSuccessListener(snapshot -> {
                int count = snapshot.size();
                Log.d("HomeFragment", "Found " + count + " posts in collection");
                
                if (count > 0) {
                    // Log each post for debugging
                    for (QueryDocumentSnapshot document : snapshot) {
                        Log.d("HomeFragment", "Post: id=" + document.getId() + 
                              ", content=" + document.getString("content") + 
                              ", imageUrl=" + document.getString("imageUrl") + 
                              ", username=" + document.getString("username"));
                    }
                }
            })
            .addOnFailureListener(e -> Log.e("HomeFragment", "Error checking post count", e));
        
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d("HomeFragment", "Query returned " + queryDocumentSnapshots.size() + " documents");
                
                feedItems.clear();
                Log.d("HomeFragment", "Cleared previous feedItems. Size now: " + feedItems.size());
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        // Log raw document data
                        Log.d("HomeFragment", "Processing document: " + document.getId());
                        Log.d("HomeFragment", "Document data: " + document.getData());
                        
                        FeedItem item = document.toObject(FeedItem.class);
                        
                        // Check for null values
                        if (item == null) {
                            Log.e("HomeFragment", "Failed to convert document to FeedItem: " + document.getId());
                            continue;
                        }
                        
                        item.setId(document.getId());
                        
                        if (currentUserId != null && item.isLikedBy(currentUserId)) {
                            item.setLikedByCurrentUser(true);
                        }
                        
                        // Debug log all properties
                        Log.d("HomeFragment", "FeedItem: id=" + item.getId() + 
                              ", content=" + item.getContent() + 
                              ", imageUrl=" + item.getImageUrl() + 
                              ", username=" + item.getUsername() +
                              ", likes=" + item.getLikes() +
                              ", liked by current user=" + item.isLikedByCurrentUser() +
                              ", timestamp=" + (item.getTimestamp() != null ? item.getTimestamp().toDate() : "null"));
                        
                        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                            feedItems.add(item);
                            Log.d("HomeFragment", "Added post to feedItems. New size: " + feedItems.size());
                        } else {
                            Log.w("HomeFragment", "Skipping post with empty image URL: " + item.getId());
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error processing document: " + document.getId(), e);
                    }
                }
                
                Log.d("HomeFragment", "Processed " + feedItems.size() + " posts");
                Log.d("HomeFragment", "Showing " + feedItems.size() + " posts");
                
                // Update UI
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d("HomeFragment", "Notifying adapter of data changes. Items count: " + feedItems.size());
                        feedAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        updateEmptyState();
                        
                        // Check empty state
                        boolean isEmpty = feedItems == null || feedItems.isEmpty();
                        Log.d("HomeFragment", "Feed is empty: " + isEmpty);
                        Log.d("HomeFragment", "EmptyStateLayout visibility: " + 
                              (emptyStateLayout.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                        Log.d("HomeFragment", "RecyclerView visibility: " + 
                              (feedRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                    });
                }
            })
            .addOnFailureListener(e -> {
                String errorMsg = e.getMessage();
                Log.e("HomeFragment", "Error loading posts: " + errorMsg, e);
                
                if (errorMsg != null && errorMsg.contains("UNAVAILABLE")) {
                    Log.w("HomeFragment", "Network appears to be unavailable, retrying in 3 seconds");
                    
                    new Handler(Looper.getMainLooper()).postDelayed(
                        this::loadFeedPosts, 
                        3000
                    );
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), "Error loading posts: " + errorMsg, Toast.LENGTH_SHORT).show();
                            updateEmptyState();
                        });
                    }
                }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


    // Debug method to check Firestore data
    private void checkPostsData(String specificPostId) {
        Log.d("HomeFragment", "DEBUGGING: Checking posts collection directly");
        
        // Fetch all posts
        db.collection("posts")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d("HomeFragment", "DEBUG: Total posts in Firestore: " + querySnapshot.size());
                
                // Look for the specific post
                boolean foundSpecificPost = false;
                
                for (DocumentSnapshot doc : querySnapshot) {
                    Map<String, Object> data = doc.getData();
                    
                    Log.d("HomeFragment", "DEBUG: Post " + doc.getId() + " data: " + data);
                    
                    if (doc.getId().equals(specificPostId)) {
                        foundSpecificPost = true;
                        Log.d("HomeFragment", "DEBUG: Found the specific post: " + specificPostId);
                        Log.d("HomeFragment", "DEBUG: Post data: " + data);
                        
                        String imageUrl = (String) data.get("imageUrl");
                        String content = (String) data.get("content");
                        String username = (String) data.get("username");
                        Timestamp timestamp = (Timestamp) data.get("timestamp");
                        
                        Log.d("HomeFragment", "DEBUG: imageUrl = " + imageUrl);
                        Log.d("HomeFragment", "DEBUG: content = " + content);
                        Log.d("HomeFragment", "DEBUG: username = " + username);
                        Log.d("HomeFragment", "DEBUG: timestamp = " + (timestamp != null ? timestamp.toDate() : "null"));
                        
                        // Check if image URL is valid
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Log.d("HomeFragment", "DEBUG: Image URL is valid");
                        } else {
                            Log.e("HomeFragment", "DEBUG: Image URL is invalid: " + imageUrl);
                        }
                    }
                }
                
                if (!foundSpecificPost) {
                    Log.e("HomeFragment", "DEBUG: Couldn't find the specific post: " + specificPostId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("HomeFragment", "DEBUG: Error fetching posts", e);
            });
    }

    private void deletePost(String postId) {
        swipeRefreshLayout.setRefreshing(true);

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Log.d("HomeFragment", "Deleting...");

                    db.collection("posts")
                            .document(postId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("HomeFragment", "Successfully deleted post: " + postId);
                                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                                removePostFromList(postId);
                                swipeRefreshLayout.setRefreshing(false);
                                updateEmptyState();

                            })
                            .addOnFailureListener(e -> {
                                Log.e("HomeFragment", "Error getting posts to delete", e);
                                Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                swipeRefreshLayout.setRefreshing(false);
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    swipeRefreshLayout.setRefreshing(false);
                })
                .show();
    }

    private void removePostFromList(String postId) {
        Iterator<FeedItem> iterator = feedItems.iterator();
        while (iterator.hasNext()) {
            FeedItem item = iterator.next();
            if (item.getId().equals(postId)) {
                iterator.remove();
                feedAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    private void handleLike(FeedItem post) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "You must be logged in to like posts", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        boolean isNowLiked = post.toggleLike(userId);
        
        // Update Firestore
        db.collection("posts").document(post.getId())
            .update("likes", post.getLikes(), "likedBy", post.getLikedBy())
            .addOnSuccessListener(aVoid -> {
                Log.d("HomeFragment", "Post " + (isNowLiked ? "liked" : "unliked") + " successfully");
            })
            .addOnFailureListener(e -> {
                post.toggleLike(userId); // Toggle back
                feedAdapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Failed to update like status", Toast.LENGTH_SHORT).show();
                Log.e("HomeFragment", "Error updating like status", e);
            });
        
        feedAdapter.notifyDataSetChanged();
    }
    
    // Method to share a post
    private void sharePost(FeedItem post) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        
        String shareText = "Check out this post from " + post.getUsername() + " on Capture!\n\n";
        if (post.getContent() != null && !post.getContent().isEmpty()) {
            shareText += post.getContent() + "\n\n";
        }
        
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            shareText += "Image: " + post.getImageUrl();
        }
        
        shareText += "\n\nDownload Capture to see more!";
        
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
    
    private void showCommentDialog(FeedItem post) {
        if (getContext() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_AlertDialog);
        builder.setTitle("Add a comment");
        
        final EditText input = new EditText(getContext());
        input.setHint("Write your comment...");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setTextColor(getResources().getColor(R.color.white));
        input.setHintTextColor(getResources().getColor(R.color.system_gray));
        
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        
        builder.setView(input);
        
        builder.setPositiveButton("Post", (dialog, which) -> {
            String commentText = input.getText().toString().trim();
            if (!commentText.isEmpty() && mAuth.getCurrentUser() != null) {
                addComment(post, commentText);
                
                for (int i = 0; i < feedItems.size(); i++) {
                    if (feedItems.get(i).getId().equals(post.getId())) {
                        feedAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            } else {
                Toast.makeText(getContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void addComment(FeedItem post, String commentText) {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        String username = mAuth.getCurrentUser().getDisplayName();
        
        post.addComment(userId, username, commentText);
        
        db.collection("posts").document(post.getId())
            .update("comments", post.getComments())
            .addOnSuccessListener(aVoid -> {
                Log.d("HomeFragment", "Comment added successfully");
                Toast.makeText(getContext(), "Comment added", Toast.LENGTH_SHORT).show();
                feedAdapter.notifyDataSetChanged(); // Refresh UI
            })
            .addOnFailureListener(e -> {
                Log.e("HomeFragment", "Error adding comment", e);
                Toast.makeText(getContext(), "Failed to add comment", Toast.LENGTH_SHORT).show();
            });
    }

    private static class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
        private final List<FeedItem> feedItems;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
        private final OnDeleteClickListener listener;


        public FeedAdapter(List<FeedItem> feedItems, OnDeleteClickListener listener) {
            this.feedItems = feedItems;
            this.listener = listener;
            setHasStableIds(true); // Ensure stable IDs for better RecyclerView performance
        }




        @Override
        public long getItemId(int position) {
            // hash the post id
            FeedItem item = feedItems.get(position);
            return item.getId() != null ? item.getId().hashCode() : position;
        }

        @NonNull
        @Override
        public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feed_post, parent, false);
            return new FeedViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
            FeedItem post = feedItems.get(position);
            holder.bind(post, dateFormat);

            holder.deletePostsButton.setOnClickListener(v -> {
                if (listener != null && post.getId() != null) {
                    listener.onDeleteClick(post.getId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return feedItems.size();
        }

        static class FeedViewHolder extends RecyclerView.ViewHolder {
            private final ImageView profileImage;
            private final TextView usernameText;
            private final TextView captionText;
            private final ImageView postImage;
            private final TextView timestampText;
            private final TextView likesText;
            private final ImageButton deletePostsButton;
            private final ImageButton likeButton;
            private final ImageButton commentButton;
            private final ImageButton shareButton;
            private final TextView commentsHeader;
            private final LinearLayout commentsContainer;
            private final TextView viewAllComments;

            public FeedViewHolder(@NonNull View itemView) {
                super(itemView);
                profileImage = itemView.findViewById(R.id.profile_image);
                usernameText = itemView.findViewById(R.id.username_text);
                captionText = itemView.findViewById(R.id.caption_text);
                postImage = itemView.findViewById(R.id.post_image);
                timestampText = itemView.findViewById(R.id.timestamp_text);
                likesText = itemView.findViewById(R.id.likes_text);
                deletePostsButton = itemView.findViewById(R.id.delete_button);
                likeButton = itemView.findViewById(R.id.like_button);
                commentButton = itemView.findViewById(R.id.comment_button);
                shareButton = itemView.findViewById(R.id.share_button);
                commentsHeader = itemView.findViewById(R.id.comments_header);
                commentsContainer = itemView.findViewById(R.id.comments_container);
                viewAllComments = itemView.findViewById(R.id.view_all_comments);
            }

            public void bind(FeedItem post, SimpleDateFormat dateFormat) {
                usernameText.setText(post.getUsername() != null ? post.getUsername() : "Anonymous");

                
                if (post.getContent() != null && !post.getContent().isEmpty()) {
                    captionText.setVisibility(View.VISIBLE);
                    captionText.setText(post.getContent());
                } else {
                    captionText.setVisibility(View.GONE);
                }

                if (post.getTimestamp() != null) {
                    Timestamp timestamp = post.getTimestamp();
                    Date date = timestamp.toDate();
                    timestampText.setText(dateFormat.format(date));
                } else {
                    timestampText.setText("Just now");
                }

                likesText.setText(String.format(Locale.getDefault(), "%d likes", post.getLikes()));

                String profileUrl = post.getProfilePictureUrl();
                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(profileUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(profileImage);
                } else {
                    Glide.with(itemView.getContext())
                            .load(R.drawable.ic_person)
                            .circleCrop()
                            .into(profileImage);
                }

                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    try {
                        String imageUrl = post.getImageUrl().trim();
                        Log.d("FeedViewHolder", "Loading post image: " + imageUrl + " for post: " + post.getId());
                        
                        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                            imageUrl = "https://" + imageUrl;
                            Log.d("FeedViewHolder", "Fixed image URL to: " + imageUrl);
                        }
                        
                        final String finalImageUrl = imageUrl;
                        postImage.setVisibility(View.VISIBLE);
                        postImage.setBackgroundColor(itemView.getResources().getColor(R.color.system_gray));
                        
                        RequestOptions requestOptions = new RequestOptions()
                                .transforms(new CenterCrop(), new RoundedCorners(16))
                                .placeholder(R.drawable.placeholder_image)
                                .error(R.drawable.error_image);
                                
                        Glide.with(itemView.getContext())
                                .load(finalImageUrl)
                                .apply(requestOptions)
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, 
                                                              Target<Drawable> target, 
                                                              boolean isFirstResource) {
                                        Log.e("FeedViewHolder", "Failed to load image: " + finalImageUrl + 
                                              ", error: " + (e != null ? e.getMessage() : "unknown"));
                                        
                                        if (finalImageUrl.startsWith("https://")) {
                                            String httpUrl = "http://" + finalImageUrl.substring(8);
                                            Log.d("FeedViewHolder", "Retrying with HTTP URL: " + httpUrl);
                                            
                                            Glide.with(itemView.getContext())
                                                .load(httpUrl)
                                                .apply(requestOptions)
                                                .into(postImage);
                                        }
                                        
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, 
                                                                  Object model, 
                                                                  Target<Drawable> target, 
                                                                  DataSource dataSource, 
                                                                  boolean isFirstResource) {
                                        Log.d("FeedViewHolder", "Successfully loaded image: " + finalImageUrl);
                                        // Clear the background when image loads
                                        postImage.setBackgroundColor(0);
                                        return false; // let Glide handle displaying the resource
                                    }
                                })
                                .into(postImage);
                    } catch (Exception e) {
                        Log.e("FeedViewHolder", "Error loading image: " + e.getMessage(), e);
                        postImage.setVisibility(View.VISIBLE);
                        Glide.with(itemView.getContext())
                                .load(R.drawable.error_image)
                                .into(postImage);
                    }
                } else {
                    Log.w("FeedViewHolder", "No image URL for post: " + post.getId());
                    postImage.setVisibility(View.GONE);
                }

                setupComments(post);

                if (post.isLikedByCurrentUser()) {
                    likeButton.setImageResource(R.drawable.ic_favorite);
                    likeButton.setColorFilter(itemView.getContext().getResources().getColor(R.color.system_red));
                } else {
                    likeButton.setImageResource(R.drawable.ic_favorite_border);
                    likeButton.setColorFilter(itemView.getContext().getResources().getColor(R.color.white));
                }
                
                likeButton.setOnClickListener(v -> {
                    if (itemView.getContext() instanceof FragmentActivity) {
                        FragmentActivity activity = (FragmentActivity) itemView.getContext();
                        HomeFragment fragment = (HomeFragment) activity.getSupportFragmentManager()
                                .findFragmentById(R.id.fragment_container);
                        
                        if (fragment != null) {
                            fragment.handleLike(post);
                        }
                    }
                });
                
                commentButton.setOnClickListener(v -> {
                    if (itemView.getContext() instanceof FragmentActivity) {
                        FragmentActivity activity = (FragmentActivity) itemView.getContext();
                        HomeFragment fragment = (HomeFragment) activity.getSupportFragmentManager()
                                .findFragmentById(R.id.fragment_container);
                        
                        if (fragment != null) {
                            fragment.showCommentDialog(post);
                        }
                    }
                });
                
                shareButton.setOnClickListener(v -> {
                    if (itemView.getContext() instanceof FragmentActivity) {
                        FragmentActivity activity = (FragmentActivity) itemView.getContext();
                        HomeFragment fragment = (HomeFragment) activity.getSupportFragmentManager()
                                .findFragmentById(R.id.fragment_container);
                        
                        if (fragment != null) {
                            fragment.sharePost(post);
                        }
                    }
                });
            }

            private void setupComments(FeedItem post) {
                commentsContainer.removeAllViews();
                
                List<Map<String, Object>> comments = post.getComments();
                if (comments == null || comments.isEmpty()) {
                    commentsHeader.setVisibility(View.GONE);
                    commentsContainer.setVisibility(View.GONE);
                    viewAllComments.setVisibility(View.GONE);
                    return;
                }
                
                commentsHeader.setVisibility(View.VISIBLE);
                commentsContainer.setVisibility(View.VISIBLE);
                
                int commentCount = comments.size();
                int commentsToShow = Math.min(commentCount, 3); // Show up to 3 comments
                
                if (commentCount > 3) {
                    viewAllComments.setVisibility(View.VISIBLE);
                    viewAllComments.setText(String.format("View all %d comments", commentCount));
                    
                    viewAllComments.setOnClickListener(v -> {
                        commentsContainer.removeAllViews();
                        for (Map<String, Object> commentData : comments) {
                            addCommentView(commentData);
                        }
                        viewAllComments.setVisibility(View.GONE);
                    });
                } else {
                    viewAllComments.setVisibility(View.GONE);
                }
                
                for (int i = 0; i < commentsToShow; i++) {
                    Map<String, Object> commentData = comments.get(i);
                    addCommentView(commentData);
                }
            }
            
            private void addCommentView(Map<String, Object> commentData) {
                // Create a new comment view
                View commentView = LayoutInflater.from(itemView.getContext())
                        .inflate(R.layout.item_comment, commentsContainer, false);
                
                // Get views
                TextView usernameView = commentView.findViewById(R.id.comment_username);
                TextView textView = commentView.findViewById(R.id.comment_text);
                
                // Set data
                String username = (String) commentData.get("username");
                String text = (String) commentData.get("text");
                
                usernameView.setText(username != null ? username : "Anonymous");
                textView.setText(text != null ? text : "");
                
                // Add to container
                commentsContainer.addView(commentView);
            }
        }
    }

    private void updateEmptyState() {
        if (feedItems == null || feedItems.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            feedRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            feedRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
