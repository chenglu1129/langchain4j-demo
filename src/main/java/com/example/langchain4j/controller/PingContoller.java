package com.example.langchain4j.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ping")
public class PingContoller {
    @GetMapping
    public Integer ping(@RequestParam String message) {
        if("ping".equalsIgnoreCase(message)) {
            return 200;
        }else
            return 0;
    }

}
