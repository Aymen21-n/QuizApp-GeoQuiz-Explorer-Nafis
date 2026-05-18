package com.example.quizapp_nafis;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quizapp_nafis.databinding.ActivityChatBinding;
import com.example.quizapp_nafis.databinding.ItemMessageBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String SYSTEM_PROMPT = "You are a helpful street guide assistant for Casablanca, Morocco. " +
            "You have knowledge about these 5 streets:\n\n" +
            "1. Avenue Hassan II: This major avenue leads directly to the city's administrative heart. A key landmark here is Mohammed V Square, a central public space designed during the French colonial era. The architecture blends European influences with traditional Moroccan styles.\n\n" +
            "2. Rue Socrates: Centrally located in areas like Maarif. A mixed-use neighborhood with apartment buildings and local shops, a typical bustling Casablanca street.\n\n" +
            "3. Boulevard d'Afghanistan: One of the main arteries in the Hay Hassani district. Home of the famous Panini Chez Hassan Star 5-star restaurant. A major redevelopment project in 2025 focused on widening the road and creating more parking.\n\n" +
            "4. Boulevard Sidi Mohammed Ben Abdellah: Runs through the upscale Marina district, close to the coast and Hassan II Mosque. Known for sea views and proximity to the waterfront.\n\n" +
            "5. Avenue des Forces Armées Royales (FAR): Connects to Place Hassan II, near the Central Market and the historic Old Medina.\n\n" +
            "Only answer questions related to these streets or Casablanca in general. For unrelated questions, politely redirect to street topics.";

    static class Message {
        String text;
        boolean isUser;

        Message(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    private List<Message> messages = new ArrayList<>();
    private List<Map<String, String>> conversationHistory = new ArrayList<>();
    private ChatAdapter adapter;
    private OkHttpClient httpClient = new OkHttpClient();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startVoiceInput();
                } else {
                    Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new ChatAdapter(messages);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(adapter);

        // Welcome message
        addMessage("Hi! I'm your Casablanca street guide. Ask me anything about the 5 streets in the quiz!", false);

        binding.btnSend.setOnClickListener(v -> sendMessage());

        binding.btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        try {
            startActivityForResult(intent, 200);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                binding.etMessage.setText(result.get(0));
            }
        }
    }

    private void addMessage(String text, boolean isUser) {
        messages.add(new Message(text, isUser));
        adapter.notifyItemInserted(messages.size() - 1);
        binding.rvMessages.scrollToPosition(messages.size() - 1);
    }

    private void sendMessage() {
        String userText = binding.etMessage.getText().toString().trim();
        if (userText.isEmpty()) return;

        addMessage(userText, true);
        binding.etMessage.setText("");

        // Add to history
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userText);
        conversationHistory.add(userMessage);

        // Typing indicator
        addMessage("...", false);

        callGroqAPI();
    }

    private void callGroqAPI() {
        try {
            JSONObject json = new JSONObject();
            json.put("model", "llama-3.3-70b-versatile");

            JSONArray messagesArray = new JSONArray();
            // System prompt
            messagesArray.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));

            // History
            for (Map<String, String> msg : conversationHistory) {
                messagesArray.put(new JSONObject(msg));
            }
            json.put("messages", messagesArray);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        removeTypingIndicator();
                        Toast.makeText(ChatActivity.this, "Connection error", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "{}";
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String responseText = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        runOnUiThread(() -> {
                            removeTypingIndicator();
                            addMessage(responseText, false);

                            Map<String, String> assistantMessage = new HashMap<>();
                            assistantMessage.put("role", "assistant");
                            assistantMessage.put("content", responseText);
                            conversationHistory.add(assistantMessage);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            removeTypingIndicator();
                            Log.e("ChatActivity", "Error parsing response: " + responseBody);
                            Toast.makeText(ChatActivity.this, "API Error", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            removeTypingIndicator();
            Toast.makeText(this, "Request error", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeTypingIndicator() {
        if (!messages.isEmpty() && messages.get(messages.size() - 1).text.equals("...")) {
            int pos = messages.size() - 1;
            messages.remove(pos);
            adapter.notifyItemRemoved(pos);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {
        private List<Message> list;

        ChatAdapter(List<Message> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Message msg = list.get(position);
            holder.binding.tvMessage.setText(msg.text);

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.binding.cardMessage.getLayoutParams();
            if (msg.isUser) {
                params.gravity = Gravity.END;
                holder.binding.cardMessage.setCardBackgroundColor(getResources().getColor(R.color.primary));
                holder.binding.tvMessage.setTextColor(Color.WHITE);
            } else {
                params.gravity = Gravity.START;
                holder.binding.cardMessage.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                holder.binding.tvMessage.setTextColor(Color.BLACK);
            }
            holder.binding.cardMessage.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ItemMessageBinding binding;
            VH(ItemMessageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
