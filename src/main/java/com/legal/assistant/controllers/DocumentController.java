package com.legal.assistant.controllers;

import com.legal.assistant.models.DocumentChunk;
import com.legal.assistant.models.DocumentRecord;
import com.legal.assistant.services.EmbeddingService;
import com.legal.assistant.services.PdfService;
import com.legal.assistant.services.QuestionService;
import com.legal.assistant.store.DocumentStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final PdfService pdfService;
    private final DocumentStore documentStore;
    private final EmbeddingService embeddingService;
    private final QuestionService questionService;

    public DocumentController(PdfService pdfService, DocumentStore documentStore, EmbeddingService embeddingService, QuestionService questionService) {
        this.pdfService    = pdfService;
        this.documentStore = documentStore;
        this.embeddingService = embeddingService;
        this.questionService = questionService;
    }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            String text                  = pdfService.extractText(file);
            List<DocumentChunk> chunks   = pdfService.chunk(text);
            embeddingService.embedChunks(chunks);

            String id = UUID.randomUUID().toString();
            documentStore.save(new DocumentRecord(id,
                    file.getOriginalFilename(), chunks));

            return ResponseEntity.ok(Map.of(
                "document_id", id,
                "filename",    file.getOriginalFilename(),
                "chunks",      chunks.size(),
                "embedded",    embeddingService.isAvailable()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/ask")
    public ResponseEntity<?> ask(@PathVariable String id,
                                  @RequestBody Map<String, String> body) {
        try {
            String question = body.get("question");
            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "question field is required"));
            }
            String answer = questionService.ask(id, question);
            return ResponseEntity.ok(Map.of(
                "document_id", id,
                "question",    question,
                "answer",      answer
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}