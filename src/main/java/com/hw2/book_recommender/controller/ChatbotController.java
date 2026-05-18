package com.hw2.book_recommender.controller;

import com.hw2.book_recommender.service.ChatbotRagService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotRagService chatRagService;

    public ChatbotController(ChatbotRagService chatRagService) {
        this.chatRagService = chatRagService;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> payload) {
        try {
            String userMessage = payload.get("message");

            String aiResponse = chatRagService.askQuestion(userMessage);

            return Map.of("reply", aiResponse);

        } catch (Exception e) {
            System.err.println("AI error: " + e.getMessage());
            e.printStackTrace();

            return Map.of("reply", "Server error: " + e.getMessage());
        }
    }
}