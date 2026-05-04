package com.legal.assistant.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_record")
public class DocumentRecord {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String filename;

    @OneToMany(mappedBy = "document",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.EAGER)
    @OrderBy("chunk_index ASC")
    private List<DocumentChunk> chunks = new ArrayList<>();

    protected DocumentRecord() {}

    public DocumentRecord(String id, String filename, List<DocumentChunk> chunks) {
        this.id       = id;
        this.filename = filename;
        for (DocumentChunk chunk : chunks) {
            chunk.setDocument(this);
            this.chunks.add(chunk);
        }
    }

    public String getId()                  { return id; }
    public String getFilename()            { return filename; }
    public List<DocumentChunk> getChunks() { return chunks; }
}
