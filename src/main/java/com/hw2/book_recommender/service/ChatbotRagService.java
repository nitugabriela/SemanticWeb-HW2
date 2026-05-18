package com.hw2.book_recommender.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.apache.jena.rdf.model.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatbotRagService {

    private BookAssistant assistant;

    public interface BookAssistant {
        @SystemMessage({
                "You are a library assistant. You have access to a book database via context.",
                "STRICT RULES:",
                "1. ONLY answer about what is explicitly in the provided context.",
                "2. When the user says 'it', 'this book', 'that book' — look at the PREVIOUS message to identify which book they mean. Then answer about THAT book only.",
                "3. NEVER switch to a different book than what was being discussed.",
                "4. NEVER use your own knowledge. Only use the context provided.",
                "5. Each book has: a title (label), an author (hasAuthor), themes (hasTheme), and a suitable reading level (suitableForReadingLevel).",
                "6. When asked to find a book by author AND theme, look for a book that matches BOTH criteria in the context.",
                "7. Example: 'What book has the author Frank Herbert and the theme Science Fiction' → find book where hasAuthor=Frank Herbert AND hasTheme=Science Fiction → answer: Dune.",
                "Reply in English."
        })
        String chat(String userMessage);
    }

    @PostConstruct
    public void init() {
        System.out.println("Starting the RAG system and Vectorization...");

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("http://localhost:1234/v1")
                .apiKey("lm-studio")
                .modelName("aldaris/gemma-3-4b-it-Q4_K_M-GGUF")
                .build();

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        List<Document> documents = extractDataFromRDF("data/books.rdf");
        System.out.println("Loaded " + documents.size() + " documents from RDF");

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(documents);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(4)
                .minScore(0.3)
                .build();

        this.assistant = AiServices.builder(BookAssistant.class)
                .chatLanguageModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        System.out.println("RAG system initialized successfully!");
    }

    public String askQuestion(String question) {
        return assistant.chat(question);
    }

    private List<Document> extractDataFromRDF(String filePath) {
        List<Document> docs = new ArrayList<>();
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream(filePath);
            if (in == null) {
                System.err.println("Error: Could not find the file " + filePath);
                return docs;
            }

            Model model = ModelFactory.createDefaultModel();
            model.read(in, null);

            ResIterator subjects = model.listSubjects();
            while (subjects.hasNext()) {
                Resource subject = subjects.nextResource();
                if (subject.isURIResource()) {
                    String label = subject.hasProperty(
                            model.getProperty("http://www.w3.org/2000/01/rdf-schema#label"))
                            ? subject.getProperty(
                            model.getProperty("http://www.w3.org/2000/01/rdf-schema#label"))
                              .getString()
                            : subject.getLocalName();

                    String typeLocal = subject.hasProperty(
                            model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
                            ? subject.getProperty(
                            model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))
                              .getResource().getLocalName()
                            : "Unknown";

                    StringBuilder sb = new StringBuilder();
                    sb.append("Name: ").append(label).append("\n");
                    sb.append("Type: ").append(typeLocal).append("\n");

                    StmtIterator properties = subject.listProperties();
                    while (properties.hasNext()) {
                        Statement stmt = properties.nextStatement();
                        String predicate = stmt.getPredicate().getLocalName();
                        String object = stmt.getObject().isLiteral()
                                ? stmt.getObject().asLiteral().getString()
                                : stmt.getObject().asResource().getLocalName();

                        if (predicate.equals("hasAuthor")) {
                            sb.append("The book '").append(label)
                                    .append("' was written by author: ").append(object).append("\n");
                        } else if (predicate.equals("suitableForReadingLevel")) {
                            sb.append("The book '").append(label)
                                    .append("' is suitable for reading level: ").append(object).append("\n");
                        } else if (predicate.equals("hasTheme")) {
                            sb.append("The book '").append(label)
                                    .append("' has theme: ").append(object).append("\n");
                        } else if (predicate.equals("hasReadingLevel")) {
                            sb.append("The user '").append(label)
                                    .append("' has reading level: ").append(object).append("\n");
                        } else if (predicate.equals("prefersTheme")) {
                            sb.append("The user '").append(label)
                                    .append("' prefers theme: ").append(object).append("\n");
                        }
                    }
                    docs.add(Document.from(sb.toString()));
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading the RDF: " + e.getMessage());
        }
        return docs;
    }
}