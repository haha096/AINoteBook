package com.example.ai_notebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping("/")
    public String index() {
        // "static/index.html"로 강제 연결해버리는 마법입니다.
        return "forward:/index.html";
    }
}
