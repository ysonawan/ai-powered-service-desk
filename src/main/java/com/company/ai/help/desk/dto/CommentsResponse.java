package com.company.ai.help.desk.dto;

public record CommentsResponse(
        String status,
        String message,
        Comments comments) {
}
