package com.example.quizapp_nafis;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp_nafis.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        binding.btnRegister.setOnClickListener(v -> registerUser());

        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Reset errors
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        // Validation
        if (name.isEmpty()) {
            binding.tilName.setError("Full name is required");
            return;
        }
        if (email.isEmpty()) {
            binding.tilEmail.setError("Email is required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Please enter a valid email");
            return;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.setError("Please confirm your password");
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        saveUserToFirestore(uid, name, email);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnRegister.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("highScore", 0);

        mFirestore.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
