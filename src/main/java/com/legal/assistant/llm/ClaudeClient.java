package com.legal.assistant.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ClaudeClient {

    private static final String API_URL    = "https://api.anthropic.com/v1/messages";
    private static final String MODEL      = "claude-sonnet-4-20250514";
    private static final int    MAX_TOKENS = 1024;
    private static final MediaType JSON    = MediaType.get("application/json");

    private final OkHttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey;

    public ClaudeClient() {
        this.apiKey = System.getenv("ANTHROPIC_API_KEY");
        this.http   = new OkHttpClient();
        this.mapper = new ObjectMapper();
    }

    public String complete(String systemPrompt, String userMessage) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", MODEL);
        body.put("max_tokens", MAX_TOKENS);
        body.put("system", systemPrompt);

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        body.set("messages", messages);

        RequestBody requestBody = RequestBody.create(
                mapper.writeValueAsString(body), JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null
                    ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Claude API error " + response.code()
                        + ": " + responseBody);
            }
            JsonNode root = mapper.readTree(responseBody);
            for (JsonNode block : root.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText();
                }
            }
            throw new IOException("No text block in response: " + root);
        }
    }
}