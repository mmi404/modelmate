package com.modelmate.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetaController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("service", "modelmate-backend", "status", "ok", "time", Instant.now().toString());
    }
}
