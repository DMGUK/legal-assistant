package com.legal.assistant.services;

import com.legal.assistant.models.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RetrievalService {

    private static final int TOP_K = 3;

    public List<DocumentChunk> retrieve(float[] queryVector,
                                         List<DocumentChunk> chunks) {
        List<Map.Entry<DocumentChunk, Double>> scored = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            if (chunk.getVector() == null) continue;
            double score = cosineSimilarity(queryVector, chunk.getVector());
            scored.add(Map.entry(chunk, score));
        }

        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<DocumentChunk> results = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_K, scored.size()); i++) {
            results.add(scored.get(i).getKey());
        }
        return results;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA      = 0.0;
        double normB      = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA      += a[i] * a[i];
            normB      += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}