package com.legal.assistant.models;

import com.legal.assistant.store.FloatArrayConverter;
import jakarta.persistence.*;

@Entity
@Table(name = "document_chunk")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_index", nullable = false)
    private int index;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Lob
    @Convert(converter = FloatArrayConverter.class)
    @Column(name = "vector")
    private float[] vector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private DocumentRecord document;

    protected DocumentChunk() {}

    public DocumentChunk(int index, String content) {
        this.index   = index;
        this.content = content;
    }

    public int getIndex()             { return index; }
    public String getContent()        { return content; }
    public DocumentRecord getDocument() { return document; }

    public void setDocument(DocumentRecord document) { this.document = document; }

    /** Returns a defensive copy so callers cannot mutate the stored vector. */
    public float[] getVector() { return vector == null ? null : vector.clone(); }

    /** Stores a defensive copy so the caller's array cannot later modify internal state. */
    public void setVector(float[] v) { this.vector = v == null ? null : v.clone(); }
}
