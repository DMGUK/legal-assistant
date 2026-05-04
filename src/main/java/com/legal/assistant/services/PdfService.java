package com.legal.assistant.services;

import com.legal.assistant.models.DocumentChunk;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfService {

    /** Reject uploads larger than this to prevent heap exhaustion. */
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB

    /** A paragraph must have at least this many characters to be stored as a chunk.
     *  Prevents single-word headings and page numbers from polluting the index. */
    private static final int MIN_CHUNK_LENGTH = 50;

    /** Flush a chunk when it reaches this length. 1000 chars ≈ ~150 words, enough
     *  to cover a full CV section (e.g. a job entry with bullet points). */
    private static final int MAX_CHUNK_LENGTH = 1000;

    /** Seed each new chunk with this many trailing characters from the previous one
     *  so context is not lost at chunk boundaries. */
    private static final int OVERLAP = 150;

    public String extractText(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File too large: " + file.getSize()
                + " bytes (max " + MAX_FILE_SIZE + " bytes)");
        }
        // RandomAccessReadBuffer wraps the InputStream for PDFBox 3.x's RandomAccessRead API.
        // Avoids a full byte[] copy via getBytes() while staying within the size guard above.
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(file.getInputStream()))) {
            return new PDFTextStripper().getText(doc);
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
                // flush current chunk at paragraph boundary if big enough
                if (current.length() > MIN_CHUNK_LENGTH) {
                    String chunkText = current.toString().strip();
                    chunks.add(new DocumentChunk(index++, chunkText));
                    // seed next chunk with overlap from end of this one
                    int overlapStart = Math.max(0, chunkText.length() - OVERLAP);
                    current = new StringBuilder(chunkText.substring(overlapStart)).append(" ");
                }
            } else {
                current.append(trimmed).append(" ");
                // flush early if chunk is getting too long
                if (current.length() > MAX_CHUNK_LENGTH) {
                    String chunkText = current.toString().strip();
                    chunks.add(new DocumentChunk(index++, chunkText));
                    int overlapStart = Math.max(0, chunkText.length() - OVERLAP);
                    current = new StringBuilder(chunkText.substring(overlapStart)).append(" ");
                }
            }
        }

        // flush remaining content
        if (current.length() > MIN_CHUNK_LENGTH) {
            chunks.add(new DocumentChunk(index++, current.toString().strip()));
        }

        return chunks;
    }
}