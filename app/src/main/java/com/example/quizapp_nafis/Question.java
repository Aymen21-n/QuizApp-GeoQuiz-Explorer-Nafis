package com.example.quizapp_nafis;

public class Question {
    private String id;
    private String imageUrl;
    private String streetName;
    private String cityId;

    public Question() {
        // Required for Firestore
    }

    public Question(String id, String imageUrl, String streetName, String cityId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.streetName = streetName;
        this.cityId = cityId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }
}
