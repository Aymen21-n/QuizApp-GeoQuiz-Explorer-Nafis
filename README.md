# GeoQuiz Explorer

## Overview
**GeoQuiz Explorer** is a native Android application built in Java, developed as a mobile development module project. The app challenges users to identify various streets in Casablanca, Morocco, through real street-level photographs. It provides an immersive experience by combining geolocation, biometric face authentication, an AI-driven chat assistant, and voice-to-text input.

## Features
- **Biometric Face Authentication**: Secure login and registration using AI-powered face recognition.
- **GPS City Detection**: Automatically detects the user's location to tailor the quiz experience.
- **Street Photo Quiz**: Interactive quiz featuring real-world street imagery integrated via the Mapillary API.
- **AI Conversational Assistant**: A helpful guide powered by the Groq LLaMA 3.3 70B model to answer questions about Casablanca streets.
- **Voice Input**: Integrated Android SpeechRecognizer allowing users to interact with the AI assistant using their voice.
- **Material Design 3**: A modern, clean, and intuitive user interface following the latest Google design standards.

## Tech Stack
- **Language**: Java
- **IDE**: Android Studio
- **Backend/Database**: Firebase Authentication, Cloud Firestore
- **Hardware Integration**: CameraX (for face capture), FusedLocationProviderClient (for GPS)
- **External APIs**: Mapillary API (street photos), Groq API (LLaMA 3.3 70B Chat)
- **Face Recognition Backend**: FastAPI, DeepFace (Facenet model), NumPy, OkHttp
- **Deployment**: Cloudflare Tunnel (for local backend exposure)

## Architecture
The application follows a distributed architecture:
1. **Android App**: Serves as the primary client interface.
2. **Firebase**: Handles user account management and stores quiz/leaderboard data.
3. **FastAPI Server**: A custom Python-based backend dedicated to biometric face embedding generation and verification.
4. **Groq API**: Processes natural language queries for the "Street Guide AI".
5. **Mapillary**: Dynamically serves street-level photographs based on coordinates.

## Setup & Installation
- **Firebase Configuration**: `google-services.json` is **NOT** included in this repository. Users must create their own project in the Firebase Console and add their specific configuration file to the `app/` directory.
- **Secrets Management**: `secrets.properties` is **NOT** included. A `secrets.properties.example` file is provided as a template. Copy this file, rename it to `secrets.properties`, and replace the placeholder with a valid Groq API key: `GROQ_API_KEY=your_real_key_here`.
- **FastAPI Backend**: The face recognition server must be running locally. Navigate to the `geoquiz_backend/` folder and execute:
  ```bash
  python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
  ```
- **Cloudflare Tunnel**: To allow the Android device to communicate with the local backend, run:
  ```bash
  cloudflared tunnel --url http://localhost:8000
  ```
  Update the generated `https://` URL in `Constants.java` as the `FASTAPI_BASE_URL`.
- **Python Dependencies**: Install necessary libraries using:
  ```bash
  pip install fastapi uvicorn deepface numpy python-multipart pillow
  ```

## FastAPI Backend
The custom backend manages biometric data through two primary endpoints:
- **POST `/register-face`**: Captures a new user's face, generates a unique face embedding using the **DeepFace Facenet** model, and stores it in `faces_db.json` keyed by the user's email.
- **POST `/verify-face`**: Compares a newly captured face image against the stored embedding using Euclidean distance. It returns `{"match": true}` if the identity is confirmed.

## Permissions Required
To function correctly, the app requires the following Android permissions:
- `INTERNET`: For Firebase, Mapillary, and AI API communication.
- `ACCESS_FINE_LOCATION`: To detect the user's proximity to Casablanca.
- `CAMERA`: To perform biometric face scans for login and registration.
- `RECORD_AUDIO`: To enable voice input features in the chat interface.

## Important Notes
- **Data Privacy**: `faces_db.json` is excluded from this repository as it contains real biometric data.
- **Cleanup**: `__pycache__` folders are excluded to keep the project clean.

## Author
**Nafis Aymen**  
4DSIO Group G3  
École Marocaine des Sciences de l'Ingénieur (EMSI)  
Academic year 2025/2026  
Supervised by: **Dr. Bousmah Mohammed**
