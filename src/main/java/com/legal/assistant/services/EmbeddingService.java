package com.legal.assistant.services;

import com.legal.assistant.embedding.DJLEmbeddingClient;
import com.legal.assistant.embedding.EmbeddingClient;
import com.legal.assistant.models.DocumentChunk;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private DJLEmbeddingClient djlClient;
    private EmbeddingClient openAiClient;

    @PostConstruct
    public void init() {
        try {
            djlClient = new DJLEmbeddingClient();
            log.info("[Embeddings] Using DJL local embeddings.");
            return;
        } catch (Exception e) {
            log.warn("[Embeddings] DJL failed: {}", e.getMessage());
        }

        String openAiKey = System.getenv("OPENAI_API_KEY");
        if (openAiKey != null && !openAiKey.isBlank()) {
            openAiClient = new EmbeddingClient(openAiKey);
            log.info("[Embeddings] Using OpenAI embeddings.");
            return;
        }

        log.warn("[Embeddings] No embedding provider available. Keyword search only.");
    }

    @PreDestroy
    public void cleanup() {
        if (djlClient != null) {
            try {
                djlClient.close();
            } catch (Exception e) {
                log.warn("[Embeddings] Error closing DJL client: {}", e.getMessage());
            }
        }
    }

    /**
     * Embeds all chunks in-place. Returns the number of chunks that failed to embed.
     * Callers should surface this count so users know the document is partially indexed.
     */
    public int embedChunks(List<DocumentChunk> chunks) {
        int failures = 0;
        if (djlClient != null) {
            for (DocumentChunk chunk : chunks) {
                try {
                    chunk.setVector(djlClient.embed(chunk.getContent()));
                } catch (Exception e) {
                    failures++;
                    log.error("[Embeddings] Failed to embed chunk {}: {}", chunk.getIndex(), e.getMessage());
                }
            }
        } else if (openAiClient != null) {
            try {
                List<String> texts = chunks.stream()
                        .map(DocumentChunk::getContent)
                        .toList();
                List<float[]> vectors = openAiClient.embedBatch(texts);
                if (vectors.size() != chunks.size()) {
                    throw new IllegalStateException("Embedding count mismatch: got "
                            + vectors.size() + " vectors for " + chunks.size() + " chunks");
                }
                for (int i = 0; i < chunks.size(); i++) {
                    chunks.get(i).setVector(vectors.get(i));
                }
            } catch (Exception e) {
                failures = chunks.size();
                log.error("[Embeddings] OpenAI batch failed: {}", e.getMessage());
            }
        }
        return failures;
    }

    public float[] embedQuery(String query) throws Exception {
        if (djlClient != null)    return djlClient.embed(query);
        if (openAiClient != null) return openAiClient.embed(query);
        throw new UnsupportedOperationException("No embedding provider available");
    }

    public boolean isAvailable() {
        return djlClient != null || openAiClient != null;
    }
}
