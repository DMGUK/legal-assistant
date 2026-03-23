package com.legal.assistant.models;

import java.util.List;

public class DocumentRecord {
    private final String id;
    private final String filename;
    private final List<DocumentChunk> chunks;

    public DocumentRecord(String id, String filename, List<DocumentChunk> chunks) {
        this.id       = id;
        this.filename = filename;
        this.chunks   = chunks;
    }

    public String getId()                  { return id; }
    public String getFilename()            { return filename; }
    public List<DocumentChunk> getChunks() { return chunks; }
}