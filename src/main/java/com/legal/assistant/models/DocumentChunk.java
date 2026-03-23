package com.legal.assistant.models;

public class DocumentChunk {
    private final int index;
    private final String content;
    private float[] vector;

    public DocumentChunk(int index, String content) {
        this.index = index;
        this.content = content;
    }

    public int getIndex()            { return index; }
    public String getContent()       { return content; }
    public float[] getVector()       { return vector; }
    public void setVector(float[] v) { this.vector = v; }
}
