package com.legal.assistant.services;

import com.legal.assistant.llm.ClaudeClient;
import com.legal.assistant.models.DocumentChunk;
import com.legal.assistant.store.DocumentStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
public class QuestionService {

    private final ClaudeClient claudeClient;
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final DocumentStore documentStore;

    @Value("classpath:prompts/system-prompt.txt")
    private Resource systemPromptResource;

    private String systemPromptTemplate;

    public QuestionService(ClaudeClient claudeClient,
                           EmbeddingService embeddingService,
                           RetrievalService retrievalService,
                           DocumentStore documentStore) {
        this.claudeClient      = claudeClient;
        this.embeddingService  = embeddingService;
        this.retrievalService  = retrievalService;
        this.documentStore     = documentStore;
    }

    @PostConstruct
    public void init() throws IOException {
        systemPromptTemplate = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public String ask(String documentId, String question) throws Exception {
        // 1. Find the document
        var record = documentStore.find(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

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
        String excerpts;
        if (chunks.isEmpty()) {
            excerpts = "No relevant content was found in the document.\n";
        } else {
            StringBuilder sb = new StringBuilder("--- DOCUMENT EXCERPTS ---\n\n");
            for (DocumentChunk chunk : chunks) {
                sb.append(chunk.getContent()).append("\n\n");
            }
            sb.append("--- END OF EXCERPTS ---\n");
            excerpts = sb.toString();
        }
        return systemPromptTemplate
                .replace("{today}", LocalDate.now().toString())
                .replace("{excerpts}", excerpts);
    }
}