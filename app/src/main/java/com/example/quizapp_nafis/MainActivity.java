package com.example.quizapp_nafis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.quizapp_nafis.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int FACE_CAPTURE_REQUEST_CODE = 101;

    private String pendingEmail;
    private String pendingPassword;
    private OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (mAuth.getCurrentUser() != null) {
            FirebaseAuth.getInstance().signOut();
        }

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Step 1: Save credentials and request Face Capture
            pendingEmail = email;
            pendingPassword = password;
            
            Intent intent = new Intent(this, FaceCaptureActivity.class);
            startActivityForResult(intent, FACE_CAPTURE_REQUEST_CODE);
        });

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == FACE_CAPTURE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String imagePath = data.getStringExtra("face_image_path");
                if (imagePath != null) {
                    verifyFaceAndLogin(imagePath);
                }
            } else {
                Toast.makeText(this, "Face capture cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void verifyFaceAndLogin(String imagePath) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        File imageFile = new File(imagePath);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", pendingEmail)
                .addFormDataPart("image", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(Constants.FASTAPI_BASE_URL + "/verify-face")
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseBody);
                    boolean match = json.getBoolean("match");

                    runOnUiThread(() -> {
                        if (match) {
                            performFirebaseLogin();
                        } else {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Face not recognized. Access denied.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void performFirebaseLogin() {
        mAuth.signInWithEmailAndPassword(pendingEmail, pendingPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startLocationFlow();
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startLocationFlow() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            detectLocationAndNavigate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                detectLocationAndNavigate();
            } else {
                navigateToQuiz(CityDetector.getDefaultCity());
            }
        }
    }

    private void detectLocationAndNavigate() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                navigateToQuiz(CityDetector.getDefaultCity());
                return;
            }

            fusedLocationClient.getLastLocation().addOnCompleteListener(this, task -> {
                try {
                    Location location = task.getResult();
                    if (location != null) {
                        navigateToQuiz(CityDetector.detectCity(location.getLatitude(), location.getLongitude()));
                    } else {
                        requestSingleLocationUpdate();
                    }
                } catch (Exception e) {
                    navigateToQuiz(CityDetector.getDefaultCity());
                }
            });
        } catch (Exception e) {
            navigateToQuiz(CityDetector.getDefaultCity());
        }
    }

    private void requestSingleLocationUpdate() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                navigateToQuiz(CityDetector.getDefaultCity());
                return;
            }

            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMaxUpdates(1)
                    .build();

            LocationCallback[] callbackHolder = new LocationCallback[1];
            callbackHolder[0] = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    fusedLocationClient.removeLocationUpdates(callbackHolder[0]);
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        navigateToQuiz(CityDetector.detectCity(location.getLatitude(), location.getLongitude()));
                    } else {
                        navigateToQuiz(CityDetector.getDefaultCity());
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, callbackHolder[0], Looper.getMainLooper());

            // Timeout after 15 seconds — go to default city
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                fusedLocationClient.removeLocationUpdates(callbackHolder[0]);
                navigateToQuiz(CityDetector.getDefaultCity());
            }, 15000);

        } catch (Exception e) {
            navigateToQuiz(CityDetector.getDefaultCity());
        }
    }

    private void navigateToQuiz(String city) {
        Intent intent = new Intent(MainActivity.this, QuizActivity.class);
        intent.putExtra("city", city);
        startActivity(intent);
        finish();
    }
}
