package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
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
        log.info("Создание события: userId={}, type={}, operation={}, entityId={}",
                userId, eventType, operation, entityId);
        try {
            Event event = Event.builder()
                    .userId(userId)
                    .eventType(eventType)
                    .operation(operation)
                    .entityId(entityId)
                    .timestamp(Instant.now().toEpochMilli())
                    .build();

            Event createdEvent = eventStorage.create(event);
            log.info("Событие успешно создано: eventId={}, userId={}, type={}",
                    createdEvent.getEventId(), userId, eventType);
            return createdEvent;
        } catch (Exception e) {
            log.error("Ошибка при создании события userId={}, type={}: {}",
                    userId, eventType, e.getMessage());
            throw new RuntimeException("Не удалось создать событие", e);
        }
    }

    public List<Event> getUserFeed(Long userId) {
        log.info("Получение ленты событий для пользователя id={}", userId);
        try {
            List<Event> events = eventStorage.findByUserId(userId);
            log.info("Успешно получено {} событий для пользователя id={}", events.size(), userId);
            return events;
        } catch (Exception e) {
            log.error("Ошибка при получении ленты событий пользователя id={}: {}",
                    userId, e.getMessage());
            throw new RuntimeException("Не удалось получить ленту событий", e);
        }
    }

    public void recordLikeEvent(Long userId, Long filmId, Event.Operation operation) {
        log.info("Запись события лайка: userId={}, filmId={}, operation={}",
                userId, filmId, operation);
        try {
            createEvent(userId, Event.EventType.LIKE, operation, filmId);
            log.info("Событие лайка успешно записано: userId={}, filmId={}, operation={}",
                    userId, filmId, operation);
        } catch (Exception e) {
            log.error("Ошибка при записи события лайка userId={}, filmId={}: {}",
                    userId, filmId, e.getMessage());
            throw new RuntimeException("Не удалось записать событие лайка", e);
        }
    }

    public void recordFriendEvent(Long userId, Long friendId, Event.Operation operation) {
        log.info("Запись события дружбы: userId={}, friendId={}, operation={}",
                userId, friendId, operation);
        try {
            createEvent(userId, Event.EventType.FRIEND, operation, friendId);
            log.info("Событие дружбы успешно записано: userId={}, friendId={}, operation={}",
                    userId, friendId, operation);
        } catch (Exception e) {
            log.error("Ошибка при записи события дружбы userId={}, friendId={}: {}",
                    userId, friendId, e.getMessage());
            throw new RuntimeException("Не удалось записать событие дружбы", e);
        }
    }

    public void recordReviewEvent(Long userId, Long reviewId, Event.Operation operation) {
        log.info("Запись события отзыва: userId={}, reviewId={}, operation={}",
                userId, reviewId, operation);
        try {
            createEvent(userId, Event.EventType.REVIEW, operation, reviewId);
            log.info("Событие отзыва успешно записано: userId={}, reviewId={}, operation={}",
                    userId, reviewId, operation);
        } catch (Exception e) {
            log.error("Ошибка при записи события отзыва userId={}, reviewId={}: {}",
                    userId, reviewId, e.getMessage());
            throw new RuntimeException("Не удалось записать событие отзыва", e);
        }
    }

    public void clearUserEvents(Long userId) {
        log.info("Очистка событий пользователя id={}", userId);
        try {
            eventStorage.deleteByUserId(userId);
            log.info("События пользователя id={} успешно очищены", userId);
        } catch (Exception e) {
            log.error("Ошибка при очистке событий пользователя id={}: {}", userId, e.getMessage());
            throw new RuntimeException("Не удалось очистить события пользователя", e);
        }
    }
}