package com.cursed.auth.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Service health and failure probes.")
public class HealthController {
    @GetMapping()
    @Operation(summary = "Health check")
    public ResponseEntity<Map> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
