package com.legal.assistant.controllers;

import com.legal.assistant.models.DocumentChunk;
import com.legal.assistant.models.DocumentRecord;
import com.legal.assistant.services.PdfService;
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

    public DocumentController(PdfService pdfService, DocumentStore documentStore) {
        this.pdfService    = pdfService;
        this.documentStore = documentStore;
    }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            String text = pdfService.extractText(file);
            List<DocumentChunk> chunks = pdfService.chunk(text);
            String id = UUID.randomUUID().toString();
            documentStore.save(new DocumentRecord(id, file.getOriginalFilename(), chunks));

            return ResponseEntity.ok(Map.of(
                "document_id", id,
                "filename",    file.getOriginalFilename(),
                "chunks",      chunks.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}