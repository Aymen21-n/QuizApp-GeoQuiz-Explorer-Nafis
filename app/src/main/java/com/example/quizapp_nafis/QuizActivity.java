package com.example.quizapp_nafis;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp_nafis.databinding.ActivityQuizBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private ActivityQuizBinding binding;
    private List<DocumentSnapshot> questions;
    private int currentIndex = 0;
    private int score = 0;
    private String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        city = getIntent().getStringExtra("city");
        if (city == null || city.isEmpty()) {
            city = "casablanca";
        }

        String displayName = city.substring(0, 1).toUpperCase() + city.substring(1).toLowerCase();
        binding.tvCityName.setText(displayName);

        binding.fabChat.setOnClickListener(v -> {
            Intent intent = new Intent(QuizActivity.this, ChatActivity.class);
            startActivity(intent);
        });

        loadQuestions();
    }

    private void loadQuestions() {
        FirebaseFirestore.getInstance()
                .collection("quizzes").document(city).collection("questions")
                .limit(5).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    questions = queryDocumentSnapshots.getDocuments();
                    if (questions.isEmpty()) {
                        Toast.makeText(this, "No questions found", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showQuestion(0);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void showQuestion(int index) {
        binding.tvQuestionCount.setText("Question " + (index + 1) + " / " + questions.size());
        binding.progressIndicator.setProgress((index * 100) / questions.size());

        DocumentSnapshot doc = questions.get(index);
        String correctAnswer = doc.getString("streetName");
        List<String> options = (List<String>) doc.get("options");

        if (options == null) {
            options = new ArrayList<>();
        }

        List<String> shuffled = new ArrayList<>(options);
        Collections.shuffle(shuffled);

        setupOptionButton(binding.btnOption1, shuffled, 0, correctAnswer);
        setupOptionButton(binding.btnOption2, shuffled, 1, correctAnswer);
        setupOptionButton(binding.btnOption3, shuffled, 2, correctAnswer);
        setupOptionButton(binding.btnOption4, shuffled, 3, correctAnswer);

        String pKey = doc.getString("pKey");
        fetchMapillaryPhoto(pKey);

        binding.tvFeedback.setVisibility(View.GONE);
        enableButtons(true);
    }

    private void setupOptionButton(MaterialButton button, List<String> options, int index, String correctAnswer) {
        if (index < options.size()) {
            String text = options.get(index);
            button.setText(text);
            button.setTag(text);
            button.setOnClickListener(v -> handleAnswer(v, correctAnswer));
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.GONE);
        }
    }

    private void fetchMapillaryPhoto(String pKey) {
        binding.loadingPhoto.setVisibility(View.GONE);
        binding.ivStreetPhoto.setVisibility(View.VISIBLE);

        WebSettings webSettings = binding.ivStreetPhoto.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        String url = "https://www.mapillary.com/embed?image_key=" + pKey + "&style=photo";
        Log.d("Mapillary", "Loading URL: " + url);
        binding.ivStreetPhoto.loadUrl(url);
    }

    private void handleAnswer(View btn, String correctAnswer) {
        enableButtons(false);
        String chosen = (String) btn.getTag();

        if (chosen != null && chosen.equals(correctAnswer)) {
            score++;
            binding.tvFeedback.setText("✓ Correct!");
            binding.tvFeedback.setTextColor(Color.parseColor("#388E3C"));
        } else {
            binding.tvFeedback.setText("✗ Wrong! It was: " + correctAnswer);
            binding.tvFeedback.setTextColor(Color.parseColor("#D32F2F"));
        }

        binding.tvFeedback.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentIndex < questions.size() - 1) {
                currentIndex++;
                showQuestion(currentIndex);
            } else {
                navigateToScore();
            }
        }, 1500);
    }

    private void enableButtons(boolean enable) {
        binding.btnOption1.setEnabled(enable);
        binding.btnOption2.setEnabled(enable);
        binding.btnOption3.setEnabled(enable);
        binding.btnOption4.setEnabled(enable);
    }

    private void navigateToScore() {
        Intent intent = new Intent(QuizActivity.this, ScoreActivity.class);
        intent.putExtra("score", score);
        intent.putExtra("total", questions.size());
        intent.putExtra("city", city);
        startActivity(intent);
        finish();
    }
}
