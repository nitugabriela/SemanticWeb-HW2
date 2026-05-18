package com.hw2.book_recommender.controller;

import com.hw2.book_recommender.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/manage")
    public String managePage(Model model) {
        try {
            model.addAttribute("books", bookService.getAllBooks());
        } catch (Exception e) {
            model.addAttribute("error", "Could not load books: " + e.getMessage());
        }
        return "manage";
    }

    @PostMapping("/add")
    public String addBook(@RequestParam String title,
                          @RequestParam String themes,
                          @RequestParam String readingLevel,
                          Model model) {
        try {
            List<String> themeList = Arrays.asList(themes.split(","));
            bookService.addBook(title, themeList, readingLevel);
            model.addAttribute("success", "Book \"" + title + "\" added successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to add book: " + e.getMessage());
        }
        try {
            model.addAttribute("books", bookService.getAllBooks());
        } catch (Exception ignored) {
        }
        return "manage";
    }

    @PostMapping("/edit")
    public String editBook(@RequestParam String bookUri,
                           @RequestParam String newReadingLevel,
                           Model model) {
        try {
            bookService.editBookLevel(bookUri, newReadingLevel);
            model.addAttribute("success", "Reading level updated successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to edit book: " + e.getMessage());
        }
        try {
            model.addAttribute("books", bookService.getAllBooks());
        } catch (Exception ignored) {
        }
        return "manage";
    }
}