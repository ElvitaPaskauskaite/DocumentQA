package com.example.documentqa;

// Test double for ChatModel: returns a fixed reply instead of calling llama3.2.
// It also keeps the last prompt it received, so tests can check what would
// have been sent to the LLM.
class FakeChatModel implements ChatModel {

    private final String reply;
    private String lastPrompt;
    private int calls = 0;

    FakeChatModel(String reply) {
        this.reply = reply;
    }

    @Override
    public String generate(String prompt) {
        calls++;
        lastPrompt = prompt;
        return reply;
    }

    String getLastPrompt() {
        return lastPrompt;
    }

    int getCalls() {
        return calls;
    }
}
