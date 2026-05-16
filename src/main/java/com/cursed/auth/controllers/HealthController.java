package com.cursed.auth.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping("/ok")
    public ResponseEntity<Map> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/fail")
    public ResponseEntity testFail() {
        return ResponseEntity.status(500).body(Map.of("error", "test"));
    }
}
