package com.lotify.lotify;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Notification {
    private final String message;
    private final LocalDateTime timestamp;
    private final NotificationType type;

    // New constructor
    public Notification(String message, NotificationType type) {
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public Notification(String message, NotificationType type, LocalDateTime timestamp) {
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public NotificationType getType() { return type; }

    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return timestamp.format(formatter);
    }

    public String getFormattedMessage() {
        return "[" + type.name() + "] " + message;
    }
}
