package com.company.ai.help.desk.dto;

public record AddCommentResultResponse(
        String status,
        String message,
        AddCommentResult addCommentResult) {
}
