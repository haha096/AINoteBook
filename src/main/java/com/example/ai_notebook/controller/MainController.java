package com.example.ai_notebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {
    @GetMapping({"/", "/login", "/signup", "/notes", "/notes/**"})
    public String index() {
        return "forward:/index.html";
    }
}
