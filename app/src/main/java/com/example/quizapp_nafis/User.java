package com.example.quizapp_nafis;

public class User {
    private String name;
    private String email;
    private int highScore;

    public User() {
        // Required for Firestore
    }

    public User(String name, String email, int highScore) {
        this.name = name;
        this.email = email;
        this.highScore = highScore;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getHighScore() {
        return highScore;
    }

    public void setHighScore(int highScore) {
        this.highScore = highScore;
    }
}
