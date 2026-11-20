package com.example.documentqa;

public class TextChunk {

    // Which document this chunk came from
    private final String sourceName;

    // The chunk's position inside that document (1, 2, 3...)
    private final int position;

    // The text of the chunk
    private final String content;

    public TextChunk(String sourceName, int position, String content) {
        this.sourceName = sourceName;
        this.position = position;
        this.content = content;
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getPosition() {
        return position;
    }

    public String getContent() {
        return content;
    }

    // A key that is unique across all documents
    public String getKey() {
        return sourceName + "#" + position;
    }
}
