package com.hw2.book_recommender.service;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class BookService {

    private static final String NS = "http://example.org/book_recommender#";
    private static final String RDF_FILE_PATH = "src/main/resources/data/books.rdf";

    private Model loadModel() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = new FileInputStream(RDF_FILE_PATH)) {
            model.read(in, null, "RDF/XML");
        }
        return model;
    }

    private void saveModel(Model model) throws IOException {
        try (OutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            model.write(out, "RDF/XML");
        }
    }

    public void addBook(String title, List<String> themes, String readingLevel) throws IOException {
        Model model = loadModel();

        String bookUri = NS + title.trim().replace(" ", "_");
        Resource book = model.createResource(bookUri);

        Property typeProperty = RDF.type;
        Property labelProperty = RDFS.label;
        Property themeProperty = model.createProperty(NS + "hasTheme");
        Property levelProperty = model.createProperty(NS + "suitableForReadingLevel");
        Resource bookClass = model.createResource(NS + "Book");

        book.addProperty(typeProperty, bookClass);
        book.addProperty(labelProperty, title.trim());
        for (String theme : themes) {
            if (!theme.isBlank()) {
                book.addProperty(themeProperty, theme.trim());
            }
        }
        book.addProperty(levelProperty, readingLevel.trim());

        saveModel(model);
    }

    public void editBookLevel(String bookUri, String newReadingLevel) throws IOException {
        Model model = loadModel();

        Resource book = model.getResource(bookUri);
        Property levelProperty = model.createProperty(NS + "suitableForReadingLevel");

        model.removeAll(book, levelProperty, null);

        book.addProperty(levelProperty, newReadingLevel.trim());

        saveModel(model);
    }

    public List<Map<String, String>> getAllBooks() throws IOException {
        Model model = loadModel();

        Property typeProperty = RDF.type;
        Property labelProperty = RDFS.label;
        Resource bookClass = model.createResource(NS + "Book");

        List<Map<String, String>> books = new ArrayList<>();
        ResIterator iter = model.listSubjectsWithProperty(typeProperty, bookClass);
        while (iter.hasNext()) {
            Resource res = iter.next();
            String uri = res.getURI();
            String label = res.hasProperty(labelProperty)
                    ? res.getProperty(labelProperty).getString()
                    : uri.substring(uri.lastIndexOf('#') + 1);
            books.add(Map.of("uri", uri, "label", label));
        }
        return books;
    }

    public Map<String, Object> getBookByUri(String bookUri) throws IOException {
        Model model = loadModel();

        Resource book = model.getResource(bookUri);
        Property labelProperty = RDFS.label;
        Property themeProperty = model.createProperty(NS + "hasTheme");
        Property levelProperty = model.createProperty(NS + "suitableForReadingLevel");

        Map<String, Object> details = new HashMap<>();
        details.put("uri", bookUri);
        details.put("label", book.hasProperty(labelProperty)
                ? book.getProperty(labelProperty).getString()
                : bookUri.substring(bookUri.lastIndexOf('#') + 1));
        details.put("readingLevel", book.hasProperty(levelProperty)
                ? book.getProperty(levelProperty).getString()
                : "Unknown");

        List<String> themes = new ArrayList<>();
        StmtIterator themeIter = book.listProperties(themeProperty);
        while (themeIter.hasNext()) {
            themes.add(themeIter.next().getString());
        }
        details.put("themes", themes);

        return details;
    }
}