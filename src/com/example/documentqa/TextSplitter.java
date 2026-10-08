package com.example.documentqa;

import java.util.ArrayList;
import java.util.List;

public class TextSplitter {

    public List<TextChunk> split(Document document) {

        List<TextChunk> chunks = new ArrayList<>();

        String[] sections =
                document.getContent().split("\\n\\s*\\n");

        int id = 1;

        for (String section : sections) {

            if (!section.trim().isEmpty()) {

                TextChunk chunk =
                        new TextChunk(id, section.trim());

                chunks.add(chunk);

                id++;
            }
        }

        return chunks;
    }
}
