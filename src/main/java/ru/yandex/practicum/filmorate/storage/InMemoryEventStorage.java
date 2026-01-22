package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryEventStorage implements EventStorage {

    private final Map<Long, Event> events = new HashMap<>();
    private long eventIdCounter = 1;

    @Override
    public Event create(Event event) {
        event.setEventId(eventIdCounter++);
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now().toEpochMilli());
        }
        events.put(event.getEventId(), event);
        return event;
    }

    @Override
    public List<Event> findByUserId(Long userId) {
        return events.values().stream()
                .filter(event -> event.getUserId().equals(userId))
                .sorted((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByUserId(Long userId) {
        List<Long> toRemove = events.values().stream()
                .filter(event -> event.getUserId().equals(userId))
                .map(Event::getEventId)
                .collect(Collectors.toList());

        toRemove.forEach(events::remove);
    }

    @Override
    public void deleteAll() {
        events.clear();
    }
}