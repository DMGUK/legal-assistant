package com.legal.assistant.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class ClaudeClient {

    private static final String API_URL    = "https://api.anthropic.com/v1/messages";
    private static final String MODEL      = "claude-sonnet-4-20250514";
    private static final int    MAX_TOKENS = 1024;
    private static final MediaType JSON    = MediaType.get("application/json");

    private final OkHttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey;

    public ClaudeClient(@Value("${ANTHROPIC_API_KEY}") String apiKey) {
        this.apiKey = apiKey;
        this.http   = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
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
            ResponseBody rawBody = response.body();
            String responseBody = rawBody != null ? rawBody.string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Claude API error " + response.code()
                        + ": " + responseBody);
            }
            JsonNode root = mapper.readTree(responseBody);
            for (JsonNode block : root.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    String text = block.path("text").asText();
                    if (!text.isEmpty()) return text;
                }
            }
            String stopReason = root.path("stop_reason").asText("unknown");
            throw new IOException("Claude returned no text content. stop_reason=" + stopReason);
        }
    }
}