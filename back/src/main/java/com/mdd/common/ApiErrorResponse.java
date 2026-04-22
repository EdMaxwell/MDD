package com.mdd.common;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error envelope returned by the API exception handler.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}
