package com.company.ai.help.desk.dto;

public record RequestTypesResponse(
        String status,
        String message,
        RequestTypes serviceDeskRequestTypes) {
}
