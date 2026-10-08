package com.example.documentqa;

public class TextChunk {

    private int id;
    private String content;

    public TextChunk(int id, String content) {
        this.id = id;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}
