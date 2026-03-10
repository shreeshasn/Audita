package com.audita.audita.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ShareController {

    // Serve index.html for any /r/** path
    // so Spring Boot doesn't 404 on direct shared URL visits
    @GetMapping("/r/**")
    public String sharedReport() {
        return "forward:/index.html";
    }
}