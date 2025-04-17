package com.pineapple.capture.friends;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.pineapple.capture.R;

public class FriendsActivity extends AppCompatActivity {
    private FriendsViewModel viewModel;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);
        
        recyclerView = findViewById(R.id.friends_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        viewModel.getFriends().observe(this, friends -> {
            // Update RecyclerView adapter with new friends list
        });
    }
} 