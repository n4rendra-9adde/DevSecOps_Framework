package com.devsecops;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "java-maven-sample");
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "name", "DevSecOps Java Sample",
            "version", "1.0.0",
            "language", "java",
            "framework", "spring-boot"
        );
    }
}
