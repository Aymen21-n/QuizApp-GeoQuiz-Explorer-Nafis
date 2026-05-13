package com.example.quizapp_nafis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
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

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (mAuth.getCurrentUser() != null) {
            startLocationFlow();
        }

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            startLocationFlow();
                        } else {
                            binding.progressBar.setVisibility(View.GONE);
                            String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
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

            // Timeout after 5 seconds — go to default city
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                fusedLocationClient.removeLocationUpdates(callbackHolder[0]);
                navigateToQuiz(CityDetector.getDefaultCity());
            }, 5000);

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
