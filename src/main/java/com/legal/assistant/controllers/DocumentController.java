package com.legal.assistant.controllers;

import com.legal.assistant.models.DocumentChunk;
import com.legal.assistant.models.DocumentRecord;
import com.legal.assistant.services.EmbeddingService;
import com.legal.assistant.services.PdfService;
import com.legal.assistant.services.QuestionService;
import com.legal.assistant.store.DocumentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final PdfService pdfService;
    private final DocumentStore documentStore;
    private final EmbeddingService embeddingService;
    private final QuestionService questionService;

    public DocumentController(PdfService pdfService, DocumentStore documentStore,
                               EmbeddingService embeddingService, QuestionService questionService) {
        this.pdfService       = pdfService;
        this.documentStore    = documentStore;
        this.embeddingService = embeddingService;
        this.questionService  = questionService;
    }

    @Transactional
    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF files are supported"));
        }
        try {
            String text                  = pdfService.extractText(file);
            List<DocumentChunk> chunks   = pdfService.chunk(text);
            int embeddingFailures        = embeddingService.embedChunks(chunks);

            String id       = UUID.randomUUID().toString();
            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "unknown";
            documentStore.save(new DocumentRecord(id, filename, chunks));

            return ResponseEntity.ok(Map.of(
                "document_id",        id,
                "filename",           filename,
                "chunks",             chunks.size(),
                "embedded",           embeddingService.isAvailable(),
                "embedding_failures", embeddingFailures
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to parse PDF: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during upload", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An internal error occurred"));
        }
    }

    @GetMapping
    public ResponseEntity<?> list() {
        var docs = documentStore.findAll().stream()
                .map(r -> Map.of(
                    "document_id", r.getId(),
                    "filename",    r.getFilename(),
                    "chunks",      r.getChunks().size()
                ))
                .toList();
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return documentStore.find(id)
                .map(r -> ResponseEntity.ok(Map.of(
                    "document_id", r.getId(),
                    "filename",    r.getFilename(),
                    "chunks",      r.getChunks().size()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (documentStore.find(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        documentStore.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ask")
    public ResponseEntity<?> ask(@PathVariable String id,
                                  @RequestBody(required = false) Map<String, String> body) {
        if (body == null || body.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Request body is required"));
        }
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "question field is required"));
        }
        try {
            String answer = questionService.ask(id, question);
            return ResponseEntity.ok(Map.of(
                "document_id", id,
                "question",    question,
                "answer",      answer
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error answering question for document {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An internal error occurred"));
        }
    }
}
