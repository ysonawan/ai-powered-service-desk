package com.company.ai.help.desk.dto;

public record ServiceDeskResponse(
        String status,
        String message,
        ServiceDesk serviceDesk) {
}
