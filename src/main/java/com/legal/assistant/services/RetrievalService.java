package com.legal.assistant.services;

import com.legal.assistant.models.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class RetrievalService {

    private static final int TOP_K = 3;

    public List<DocumentChunk> retrieve(float[] queryVector,
                                         List<DocumentChunk> chunks) {
        // Min-heap keyed by score: the smallest score is evicted once the heap exceeds TOP_K,
        // leaving only the TOP_K highest-scoring chunks. O(n log k) vs O(n log n) for full sort.
        PriorityQueue<Map.Entry<DocumentChunk, Double>> heap =
                new PriorityQueue<>(TOP_K + 1, Map.Entry.comparingByValue());

        for (DocumentChunk chunk : chunks) {
            if (chunk.getVector() == null) continue;
            if (chunk.getVector().length != queryVector.length) continue; // dimension mismatch guard
            double score = cosineSimilarity(queryVector, chunk.getVector());
            heap.offer(Map.entry(chunk, score));
            if (heap.size() > TOP_K) heap.poll(); // evict lowest score
        }

        // Heap yields ascending order; reverse so highest score is first.
        List<DocumentChunk> results = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) results.add(heap.poll().getKey());
        Collections.reverse(results);
        return results;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                "Vector dimension mismatch: " + a.length + " vs " + b.length);
        }
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