# Document Q&A System

![Tests](https://github.com/ElvitaPaskauskaite/DocumentQA/actions/workflows/ci.yml/badge.svg)

A Java-based document question-answering system built as a
learning project in Object-Oriented Programming.

## Project Goal

The goal of this project is to build a system that can
answer questions based on information contained within
provided documents.

## How it works
question → semantic search → top chunks → prompt → LLM → answer

## Current Features

- Loads every `.txt` file in a folder (`documents/`)
- Splits documents into paragraph chunks, with a maximum size and overlapping word windows for long paragraphs
- Keyword search (first version, kept for comparison)
- Semantic search: embeddings from Ollama (`nomic-embed-text`) compared with hand-written cosine similarity
- `EmbeddingModel`, `ChatModel` and `VectorStore` interfaces, so implementations can be swapped
- Answers from `llama3.2` using only the retrieved chunks, with source citations, and "I don't know" when the answer is not in the documents
- Embedding cache saved to a JSON file (indexing 39 chunks: 1136 ms without cache, 2 ms with cache, one run on my laptop)
- Settings in `application.properties`

The example documents are fictional company policies for "Acme Ltd".

## Planned Features

- PDF support
- REST API

# Roadmap

1. Basic Java - load text, chunk keyword search (done)
2. Semantic search - embedding and vector similarity (done)
3. LLM integration - generate answers from retrieved chunks (RAG) (done)
4. Spring Boot API - 'POST /documents' and 'POST /questions'
5. Production Features - PDF upload upload, PostGreSQL, pgvector, Docker tests, frontend

## Technologies and requirements

- Java 21 and Maven
- JUnit 5 for tests, GitHub Actions for continuous integration
- [Ollama](https://ollama.com) installed and running, with the models pulled:

```bash
ollama pull nomic-embed-text
ollama pull llama3.2
```

## Running the tests

```bash
mvn test
```

74 tests: 71 unit tests use test doubles instead of Ollama, so they run anywhere, including on GitHub Actions. 3 integration tests call the real Ollama server and are skipped when it is not running.