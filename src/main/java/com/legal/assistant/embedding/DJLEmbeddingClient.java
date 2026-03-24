package com.legal.assistant.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.LongBuffer;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class DJLEmbeddingClient implements AutoCloseable {

    private static final String MODEL_URL =
        "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2" +
        "/resolve/main/onnx/model.onnx?download=true";
    private static final String TOKENIZER_URL =
        "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2" +
        "/resolve/main/tokenizer.json?download=true";
    private static final Path CACHE_DIR =
        Path.of(System.getProperty("user.home"), ".cache", "legal-assistant");
    private static final Path MODEL_PATH     = CACHE_DIR.resolve("model.onnx");
    private static final Path TOKENIZER_PATH = CACHE_DIR.resolve("tokenizer.json");
    private static final int MAX_LENGTH = 128;

    private final OrtEnvironment env;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;

    public DJLEmbeddingClient() throws Exception {
        System.out.println("Loading local embedding model...");
        Files.createDirectories(CACHE_DIR);

        downloadIfMissing(MODEL_PATH, MODEL_URL, "model (~90MB)");
        downloadIfMissing(TOKENIZER_PATH, TOKENIZER_URL, "tokenizer");

        this.env       = OrtEnvironment.getEnvironment();
        this.session   = env.createSession(MODEL_PATH.toString());
        this.tokenizer = HuggingFaceTokenizer.newInstance(TOKENIZER_PATH,
                Map.of("maxLength", String.valueOf(MAX_LENGTH),
                       "padding",   "true",
                       "truncation","true"));

        System.out.println("Local embedding model ready.");
    }

    private void downloadIfMissing(Path path, String url, String name) throws Exception {
        if (!Files.exists(path)) {
            System.out.println("Downloading " + name + " (one time only)...");
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Java-HttpClient")
                    .build();
            HttpResponse<Path> response = client.send(request,
                    HttpResponse.BodyHandlers.ofFile(path));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(path);
                throw new Exception("Failed to download " + name
                        + ": HTTP " + response.statusCode());
            }
            System.out.println(name + " downloaded.");
        }
    }

    public float[] embed(String text) throws Exception {
        Encoding encoding = tokenizer.encode(text);

        long[] inputIds      = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        long[] tokenTypeIds  = encoding.getTypeIds();
        long[] shape         = {1, inputIds.length};

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids",
                OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), shape));
        inputs.put("attention_mask",
                OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape));
        inputs.put("token_type_ids",
                OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds), shape));

        try (OrtSession.Result result = session.run(inputs)) {
            float[][][] output = (float[][][]) result.get(0).getValue();
            return meanPool(output[0], attentionMask);
        }
    }

    private float[] meanPool(float[][] tokenEmbeddings, long[] attentionMask) {
        int dims      = tokenEmbeddings[0].length;
        float[] pooled = new float[dims];
        float maskSum  = 0;

        for (int i = 0; i < tokenEmbeddings.length; i++) {
            float mask = attentionMask[i];
            maskSum += mask;
            for (int j = 0; j < dims; j++) {
                pooled[j] += tokenEmbeddings[i][j] * mask;
            }
        }

        float norm = 0;
        for (int j = 0; j < dims; j++) {
            pooled[j] /= Math.max(maskSum, 1e-9f);
            norm += pooled[j] * pooled[j];
        }
        norm = (float) Math.sqrt(norm);
        for (int j = 0; j < dims; j++) {
            pooled[j] /= Math.max(norm, 1e-9f);
        }
        return pooled;
    }

    @Override
    public void close() throws Exception {
        session.close();
        env.close();
        tokenizer.close();
    }
}