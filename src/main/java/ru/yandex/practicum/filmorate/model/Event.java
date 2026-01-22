package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Event {
    private Long eventId;
    private Long userId;
    private Long entityId;
    private EventType eventType;
    private Operation operation;
    private LocalDateTime timestamp;
}