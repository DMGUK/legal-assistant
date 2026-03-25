package com.legal.assistant.services;

import com.legal.assistant.llm.ClaudeClient;
import com.legal.assistant.models.DocumentChunk;
import com.legal.assistant.store.DocumentStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final ClaudeClient claudeClient;
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final DocumentStore documentStore;

    public QuestionService(ClaudeClient claudeClient,
                           EmbeddingService embeddingService,
                           RetrievalService retrievalService,
                           DocumentStore documentStore) {
        this.claudeClient      = claudeClient;
        this.embeddingService  = embeddingService;
        this.retrievalService  = retrievalService;
        this.documentStore     = documentStore;
    }

    public String ask(String documentId, String question) throws Exception {
        // 1. Find the document
        var record = documentStore.find(documentId)
                .orElseThrow(() -> new Exception("Document not found: " + documentId));

        // 2. Embed the question
        float[] queryVector = embeddingService.embedQuery(question);

        // 3. Retrieve most relevant chunks
        List<DocumentChunk> relevant = retrievalService.retrieve(
                queryVector, record.getChunks());

        // 4. Build grounded system prompt
        String systemPrompt = buildSystemPrompt(relevant);

        // 5. Ask Claude
        return claudeClient.complete(systemPrompt, question);
    }

    private String buildSystemPrompt(List<DocumentChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a legal document assistant.\n");
        sb.append("Answer the user's question using ONLY the document excerpts below.\n");
        sb.append("If the answer is not present in the excerpts, say so clearly.\n");
        sb.append("Do NOT guess or invent information not found in the document.\n\n");

        if (chunks.isEmpty()) {
            sb.append("No relevant content was found in the document.\n");
        } else {
            sb.append("--- DOCUMENT EXCERPTS ---\n\n");
            for (DocumentChunk chunk : chunks) {
                sb.append(chunk.getContent()).append("\n\n");
            }
            sb.append("--- END OF EXCERPTS ---\n");
        }
        return sb.toString();
    }
}