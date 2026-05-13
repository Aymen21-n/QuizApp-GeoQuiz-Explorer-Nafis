package com.example.quizapp_nafis;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizapp_nafis.databinding.ActivityScoreBinding;
import com.example.quizapp_nafis.databinding.ItemLeaderboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ScoreActivity extends AppCompatActivity {

    private ActivityScoreBinding binding;
    private LeaderboardAdapter adapter;

    static class LeaderboardEntry {
        String name;
        int highScore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int score = getIntent().getIntExtra("score", 0);
        int total = getIntent().getIntExtra("total", 5);
        String city = getIntent().getStringExtra("city");
        if (city == null || city.isEmpty()) city = "casablanca";

        binding.tvScore.setText(score + " / " + total);

        if (score == total) {
            binding.tvMessage.setText("Perfect! 🏆");
        } else if (score >= total * 0.8) {
            binding.tvMessage.setText("Excellent! 🌟");
        } else if (score >= total * 0.6) {
            binding.tvMessage.setText("Good job! 👍");
        } else {
            binding.tvMessage.setText("Keep exploring! 🗺️");
        }

        int progress = (total > 0) ? (score * 100) / total : 0;
        ObjectAnimator.ofInt(binding.scoreProgress, "progress", 0, progress)
                .setDuration(1000)
                .start();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    long currentHigh = 0;
                    if (doc.exists() && doc.contains("highScore")) {
                        currentHigh = doc.getLong("highScore");
                    }

                    if (score > currentHigh) {
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                .update("highScore", score);
                        binding.tvHighScore.setText("New Best: " + score + " / " + total + " 🎉");
                    } else {
                        binding.tvHighScore.setText("Your Best: " + currentHigh + " / " + total);
                    }
                });

        binding.rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(new ArrayList<>());
        binding.rvLeaderboard.setAdapter(adapter);

        loadLeaderboard();

        binding.btnPlayAgain.setOnClickListener(v -> {
            Intent intent = new Intent(ScoreActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        binding.btnChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ScoreActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadLeaderboard() {
        FirebaseFirestore.getInstance().collection("users")
                .orderBy("highScore", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<LeaderboardEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        LeaderboardEntry e = new LeaderboardEntry();
                        e.name = doc.getString("name");
                        Long hs = doc.getLong("highScore");
                        e.highScore = (hs != null) ? hs.intValue() : 0;
                        list.add(e);
                    }
                    adapter.updateList(list);
                });
    }

    private class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {
        private List<LeaderboardEntry> list;

        LeaderboardAdapter(List<LeaderboardEntry> list) {
            this.list = list;
        }

        void updateList(List<LeaderboardEntry> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            LeaderboardEntry entry = list.get(position);
            holder.binding.tvRank.setText("#" + (position + 1));
            holder.binding.tvName.setText(entry.name);
            holder.binding.tvScore.setText(entry.highScore + " pts");
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ItemLeaderboardBinding binding;

            VH(ItemLeaderboardBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
