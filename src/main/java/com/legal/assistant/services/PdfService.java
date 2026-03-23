package com.legal.assistant.services;

import com.legal.assistant.models.DocumentChunk;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfService {

    public String extractText(MultipartFile file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    public List<DocumentChunk> chunk(String text) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        
        StringBuilder current = new StringBuilder();
        int index = 0;

        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                // flush current chunk if big enough
                if (current.length() > 50) {
                    chunks.add(new DocumentChunk(index++, current.toString().strip()));
                    current = new StringBuilder();
                }
            } else {
                current.append(trimmed).append(" ");
                // flush if chunk is getting long enough
                if (current.length() > 300) {
                    chunks.add(new DocumentChunk(index++, current.toString().strip()));
                    current = new StringBuilder();
                }
            }
        }

        // flush remaining content
        if (current.length() > 50) {
            chunks.add(new DocumentChunk(index++, current.toString().strip()));
        }

        return chunks;
    }
}