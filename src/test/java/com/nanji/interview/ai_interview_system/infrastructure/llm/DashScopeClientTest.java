package com.nanji.interview.ai_interview_system.infrastructure.llm;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DashScopeClientTest {
    private final DashScopeProperties properties = new DashScopeProperties();
    private final DashScopeClient client = new DashScopeClient(properties);

    private boolean shouldFallback(int status, String body) {
        return (boolean) ReflectionTestUtils.invokeMethod(client, "shouldFallback", status, body);
    }

    @Test void authenticationAndRequestConfigurationErrorsDoNotFallback() {
        assertFalse(shouldFallback(401, "invalid api key"));
        assertFalse(shouldFallback(400, "invalid request"));
        assertFalse(shouldFallback(404, "model not found"));
        assertFalse(shouldFallback(403, "permission denied"));
    }

    @Test void onlyConfirmedQuotaForbiddenFallsBack() {
        assertTrue(shouldFallback(403, "AllocationQuota.FreeTierOnly"));
        assertTrue(shouldFallback(403, "quota exhausted"));
        assertFalse(shouldFallback(403, "account permission denied"));
    }

    @Test void throttlingAndServerErrorsFallback() {
        assertTrue(shouldFallback(429, "too many requests"));
        assertTrue(shouldFallback(500, "service unavailable"));
        assertTrue(shouldFallback(503, "temporary error"));
        assertFalse(shouldFallback(200, "ok"));
    }

    @Test void modelPoolKeepsConfiguredOrderAndRemovesDuplicates() {
        properties.setModel("qwen-plus");
        properties.setFallbackModels(List.of("qwen-flash", "qwen-plus", "", "qwen-flash"));
        assertEquals(List.of("qwen-plus", "qwen-flash"), properties.getAllModels());
    }
}

