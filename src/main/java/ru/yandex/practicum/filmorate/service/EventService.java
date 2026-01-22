package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventStorage eventStorage;

    public void addEvent(Long userId, Long entityId, EventType eventType, Operation operation) {
        log.info("Создание события: userId={}, entityId={}, eventType={}, operation={}",
                userId, entityId, eventType, operation);

        Event event = new Event();
        event.setUserId(userId);
        event.setEntityId(entityId);
        event.setEventType(eventType);
        event.setOperation(operation);
        event.setTimestamp(LocalDateTime.now());

        eventStorage.addEvent(event);
        log.debug("Событие создано: eventId={}", event.getEventId());
    }

    public List<Event> getEventsByUserId(Long userId) {
        log.info("Запрос ленты событий для пользователя с id={}", userId);
        return eventStorage.getEventsByUserId(userId);
    }
}