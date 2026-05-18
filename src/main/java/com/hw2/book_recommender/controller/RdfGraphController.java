package com.hw2.book_recommender.controller;

import com.hw2.book_recommender.service.RdfGraphService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class RdfGraphController {

    private final RdfGraphService rdfGraphService;

    public RdfGraphController(RdfGraphService rdfGraphService) {
        this.rdfGraphService = rdfGraphService;
    }

    @GetMapping("/graph")
    public String index() {
        return "graph";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model springModel) {
        if (file.isEmpty()) {
            springModel.addAttribute("error", "Please select a file to upload.");
            return "graph";
        }

        try {
            String base64Image = rdfGraphService.processAndVisualizeRdf(file);
            springModel.addAttribute("graphImage", "data:image/png;base64," + base64Image);

        } catch (Exception e) {
            springModel.addAttribute("error", "Failed to parse or visualize RDF: " + e.getMessage());
        }

        return "graph";
    }
}

