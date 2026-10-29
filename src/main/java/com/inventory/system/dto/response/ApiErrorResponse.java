package com.inventory.system.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard RFC 7807 Problem Details error response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    private Map<String, String> validationErrors;
}
