package com.example.quizapp_nafis;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quizapp_nafis.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private OkHttpClient okHttpClient = new OkHttpClient();

    private String name, email, password;
    private static final int FACE_CAPTURE_REQUEST_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        binding.btnRegister.setOnClickListener(v -> validateAndRegister());

        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void validateAndRegister() {
        name = binding.etName.getText().toString().trim();
        email = binding.etEmail.getText().toString().trim();
        password = binding.etPassword.getText().toString().trim();
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

        // 1. Create Firebase User
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 2. Launch FaceCaptureActivity
                        Intent intent = new Intent(this, FaceCaptureActivity.class);
                        startActivityForResult(intent, FACE_CAPTURE_REQUEST_CODE);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnRegister.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FACE_CAPTURE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String imagePath = data.getStringExtra("face_image_path");
                if (imagePath != null) {
                    registerFaceOnServer(imagePath);
                }
            } else {
                handleRegistrationFailure("Face capture required for registration.");
            }
        }
    }

    private void registerFaceOnServer(String imagePath) {
        File imageFile = new File(imagePath);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", email)
                .addFormDataPart("image", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(Constants.FASTAPI_BASE_URL + "/register-face")
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleRegistrationFailure("Face registration connection error");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(responseBody);
                    if (json.optString("status").equals("registered")) {
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Face registered!", Toast.LENGTH_SHORT).show());
                        saveUserToFirestore();
                    } else {
                        handleRegistrationFailure("Could not detect face. Please try again.");
                    }
                } catch (Exception e) {
                    handleRegistrationFailure("Server registration error");
                }
            }
        });
    }

    private void handleRegistrationFailure(String message) {
        runOnUiThread(() -> {
            if (mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().delete();
            }
            binding.progressBar.setVisibility(View.GONE);
            binding.btnRegister.setEnabled(true);
            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void saveUserToFirestore() {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("highScore", 0);

        mFirestore.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    runOnUiThread(() -> {
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
    }
}
