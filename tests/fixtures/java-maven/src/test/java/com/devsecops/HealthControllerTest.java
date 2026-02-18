package com.devsecops;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    void healthEndpointReturnsUp() {
        Map<String, String> result = controller.health();
        assertEquals("UP", result.get("status"));
    }

    @Test
    void infoEndpointReturnsCorrectLanguage() {
        Map<String, Object> result = controller.info();
        assertEquals("java", result.get("language"));
        assertEquals("1.0.0", result.get("version"));
    }
}
