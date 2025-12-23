package com.company.ai.help.desk.dto;

public record ServiceDeskTicketResponse(
        String status,
        String message,
        ServiceDeskTicket serviceDeskTicket) {
}
