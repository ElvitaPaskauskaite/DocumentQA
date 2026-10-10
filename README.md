# Document Q&A System

A Java-based document question-answering system built as a
learning project in Object-Oriented Programming.

## Project Goal

The goal of this project is to build a system that can
answer questions based on information contained within
provided documents.

## Current Features

- Basic document loading
- Splits document into paragraph_based chunks
- Keyword search that returns matching chunks
- Cosine similarity ('VectorMath') for comparing embedding vector

## Planned Features

- Embeddings
- LLM integration
- PDF support
- REST API

# Roadmap

1. Basic Java - load text, chunk keyword search (done)
2. Semantic search - embedding and vector similarity ( in progress)
3. LLM integration - generate answers from retrieved chunks (RAG)
4. Spring Boot API - 'POST /documents' and 'POST /questions'
5. Production Features - PDF upload upload, PostGreSQL, pgvector, Docker tests, frontend

## Technologies

- Java
- Git / GitHub