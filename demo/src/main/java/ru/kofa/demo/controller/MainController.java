package ru.kofa.demo.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.kofa.demo.service.IProductService;

import java.util.Map;

@Controller
@RequestMapping("/main")
@AllArgsConstructor
public class MainController {
    private IProductService productService;

    @GetMapping()
    public String getWebMain() {
        return "main.html";
    }

    @PostMapping("/analiz")
    @ResponseBody
    public Map<String, Object> analyzeProduct(@RequestBody String request) {
        String cleanedRequest = request.replace("\"", "").trim();
        return productService.analytics(cleanedRequest.replace(" ", ""));
    }
}
