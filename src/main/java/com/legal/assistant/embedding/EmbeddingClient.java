package com.legal.assistant.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmbeddingClient {

    private static final String API_URL = "https://api.openai.com/v1/embeddings";
    private static final String MODEL   = "text-embedding-3-small";
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey;

    public EmbeddingClient(String apiKey) {
        this.apiKey  = apiKey;
        this.http    = new OkHttpClient();
        this.mapper  = new ObjectMapper();
    }

    public float[] embed(String text) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", MODEL);
        body.put("input", text);

        RequestBody requestBody = RequestBody.create(
                mapper.writeValueAsString(body), JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null
                    ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI error " + response.code()
                        + ": " + responseBody);
            }
            JsonNode root = mapper.readTree(responseBody);
            JsonNode embedding = root.path("data").get(0).path("embedding");

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            return vector;
        }
    }

    public List<float[]> embedBatch(List<String> texts) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", MODEL);
        ArrayNode inputArray = mapper.createArrayNode();
        for (String text : texts) inputArray.add(text);
        body.set("input", inputArray);

        RequestBody requestBody = RequestBody.create(
                mapper.writeValueAsString(body), JSON);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null
                    ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI error " + response.code()
                        + ": " + responseBody);
            }
            JsonNode root = mapper.readTree(responseBody);
            JsonNode data = root.path("data");

            List<float[]> vectors = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embedding = item.path("embedding");
                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = (float) embedding.get(i).asDouble();
                }
                vectors.add(vector);
            }
            return vectors;
        }
    }
}