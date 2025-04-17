package com.pineapple.capture.profile;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.pineapple.capture.R;

public class ProfileActivity extends AppCompatActivity {
    private ProfileViewModel viewModel;
    private ImageView profileImage;
    private TextView displayName;
    private TextView userName;
    private TextView userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        profileImage = findViewById(R.id.profile_image);
        displayName = findViewById(R.id.user_name);
        userName = findViewById(R.id.username);
        userEmail = findViewById(R.id.user_email);

        // Observe user data changes
        viewModel.getUserData().observe(this, user -> {
            if (user != null) {
                displayName.setText(user.getDisplayName());
                userName.setText(user.getUsername());
                userEmail.setText(user.getEmail());
                String profileUrl = user.getPrimaryProfilePictureUrl();
                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(this)
                            .load(profileUrl)
                            .circleCrop()
                            .into(profileImage);
                }
            }
        });
    }
} 