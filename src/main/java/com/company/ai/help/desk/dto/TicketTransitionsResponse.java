package com.company.ai.help.desk.dto;

public record TicketTransitionsResponse(
        String status,
        String message,
        TicketTransitions ticketTransitions) {
}
