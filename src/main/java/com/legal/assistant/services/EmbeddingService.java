package com.legal.assistant.services;

import com.legal.assistant.embedding.DJLEmbeddingClient;
import com.legal.assistant.embedding.EmbeddingClient;
import com.legal.assistant.models.DocumentChunk;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private DJLEmbeddingClient djlClient;
    private EmbeddingClient openAiClient;
    private boolean useDjl = false;
    private boolean useOpenAi = false;

    @PostConstruct
    public void init() {
        // Try DJL first
        try {
            djlClient = new DJLEmbeddingClient();
            useDjl = true;
            System.out.println("[Embeddings] Using DJL local embeddings.");
            return;
        } catch (Exception e) {
            System.err.println("[Embeddings] DJL failed: " + e.getMessage());
        }

        // Try OpenAI
        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey != null && !openAiKey.isBlank()) {
            openAiClient = new EmbeddingClient(openAiKey);
            useOpenAi = true;
            System.out.println("[Embeddings] Using OpenAI embeddings.");
            return;
        }

        System.out.println("[Embeddings] No embedding provider available. Keyword search only.");
    }

    public void embedChunks(List<DocumentChunk> chunks) {
        if (useDjl) {
            for (DocumentChunk chunk : chunks) {
                try {
                    chunk.setVector(djlClient.embed(chunk.getContent()));
                } catch (Exception e) {
                    System.err.println("[Embeddings] Failed to embed chunk "
                            + chunk.getIndex() + ": " + e.getMessage());
                }
            }
        } else if (useOpenAi) {
            try {
                List<String> texts = chunks.stream()
                        .map(DocumentChunk::getContent)
                        .toList();
                List<float[]> vectors = openAiClient.embedBatch(texts);
                for (int i = 0; i < chunks.size(); i++) {
                    chunks.get(i).setVector(vectors.get(i));
                }
            } catch (Exception e) {
                System.err.println("[Embeddings] OpenAI batch failed: " + e.getMessage());
            }
        }
    }

    public float[] embedQuery(String query) throws Exception {
        if (useDjl)     return djlClient.embed(query);
        if (useOpenAi)  return openAiClient.embed(query);
        throw new Exception("No embedding provider available.");
    }

    public boolean isAvailable() {
        return useDjl || useOpenAi;
    }
}