package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventStorage eventStorage;

    public Event createEvent(Long userId, Event.EventType eventType,
                             Event.Operation operation, Long entityId) {
        log.debug("Создание события: userId={}, type={}, operation={}, entityId={}",
                userId, eventType, operation, entityId);

        Event event = Event.builder()
                .userId(userId)
                .eventType(eventType)
                .operation(operation)
                .entityId(entityId)
                .timestamp(Instant.now().toEpochMilli())
                .build();

        return eventStorage.create(event);
    }

    public List<Event> getUserFeed(Long userId) {
        log.info("Получение ленты событий для пользователя id={}", userId);
        List<Event> events = eventStorage.findByUserId(userId);
        log.debug("Найдено событий для пользователя id={}: {}", userId, events.size());
        return events;
    }

    public void recordLikeEvent(Long userId, Long filmId, Event.Operation operation) {
        createEvent(userId, Event.EventType.LIKE, operation, filmId);
        log.info("Записано событие лайка: userId={}, filmId={}, operation={}",
                userId, filmId, operation);
    }

    public void recordFriendEvent(Long userId, Long friendId, Event.Operation operation) {
        createEvent(userId, Event.EventType.FRIEND, operation, friendId);
        log.info("Записано событие дружбы: userId={}, friendId={}, operation={}",
                userId, friendId, operation);
    }

    public void recordReviewEvent(Long userId, Long reviewId, Event.Operation operation) {
        createEvent(userId, Event.EventType.REVIEW, operation, reviewId);
        log.info("Записано событие отзыва: userId={}, reviewId={}, operation={}",
                userId, reviewId, operation);
    }

    public void clearUserEvents(Long userId) {
        log.info("Очистка событий пользователя id={}", userId);
        eventStorage.deleteByUserId(userId);
    }
}