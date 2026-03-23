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
        String[] paragraphs = text.split("\\n\\n+");
        int index = 0;
        for (String para : paragraphs) {
            String trimmed = para.strip();
            if (trimmed.length() > 50) {
                chunks.add(new DocumentChunk(index++, trimmed));
            }
        }
        return chunks;
    }
}