package com.adamnestor.courtvision.test.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/protected")
    public String protectedEndpoint() {
        return "protected";
    }

    @GetMapping("/api/test/public")
    public String publicEndpoint() {
        return "public";
    }
}