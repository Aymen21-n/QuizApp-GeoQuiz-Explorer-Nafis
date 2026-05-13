package com.example.quizapp_nafis;

import android.location.Location;

public class CityDetector {

    // TODO: add Tanger and El Jadida when ready
    private static class City {
        String id;
        double lat;
        double lng;

        City(String id, double lat, double lng) {
            this.id = id;
            this.lat = lat;
            this.lng = lng;
        }
    }

    private static final City CASABLANCA = new City("casablanca", 33.5731, -7.5898);

    public static String detectCity(double userLat, double userLng) {
        float[] results = new float[1];
        Location.distanceBetween(userLat, userLng, CASABLANCA.lat, CASABLANCA.lng, results);
        // Requirement says always returns "casablanca" since it's the only city
        return CASABLANCA.id;
    }

    public static String getDefaultCity() {
        return CASABLANCA.id;
    }
}
