package com.med.chat.model;

import lombok.Builder;

@Builder
public record ChatMessage(
        String content,
        String sender,
        MessageType type
) {
}

