package com.hw2.book_recommender.controller;

import com.hw2.book_recommender.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class BookListController {

    private final BookService bookService;

    public BookListController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/books")
    public String listBooks(Model model) {
        try {
            model.addAttribute("books", bookService.getAllBooks());
        } catch (Exception e) {
            model.addAttribute("error", "Could not load books: " + e.getMessage());
        }
        return "book-list";
    }

    @GetMapping("/books/detail")
    public String bookDetail(@RequestParam("uri") String encodedUri, Model model) {
        try {
            String bookUri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8);
            model.addAttribute("book", bookService.getBookByUri(bookUri));
        } catch (Exception e) {
            model.addAttribute("error", "Could not load book: " + e.getMessage());
        }
        return "book-detail";
    }
}