package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Event create(Event event) {
        log.debug("Создание события: userId={}, type={}, operation={}, entityId={}",
                event.getUserId(), event.getEventType(), event.getOperation(), event.getEntityId());

        String sql = """
                INSERT INTO events (user_id, event_type, operation, entity_id, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        long timestamp = event.getTimestamp() != null ? event.getTimestamp() : Instant.now().toEpochMilli();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"event_id"});
            stmt.setLong(1, event.getUserId());
            stmt.setString(2, event.getEventType().name());
            stmt.setString(3, event.getOperation().name());
            stmt.setLong(4, event.getEntityId());
            stmt.setTimestamp(5, new Timestamp(timestamp));
            return stmt;
        }, keyHolder);

        event.setEventId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        event.setTimestamp(timestamp);

        log.debug("Событие создано с id={}", event.getEventId());
        return event;
    }

    @Override
    public List<Event> findByUserId(Long userId) {
        log.debug("Поиск событий для пользователя id={}", userId);

        String sql = """
                SELECT * FROM events
                WHERE user_id = ?
                ORDER BY timestamp DESC
                """;

        List<Event> events = jdbcTemplate.query(sql, this::mapRowToEvent, userId);
        log.debug("Найдено событий: {}", events.size());

        return events;
    }

    @Override
    public void deleteByUserId(Long userId) {
        log.debug("Удаление всех событий пользователя id={}", userId);
        String sql = "DELETE FROM events WHERE user_id = ?";
        int deleted = jdbcTemplate.update(sql, userId);
        log.debug("Удалено событий: {}", deleted);
    }

    @Override
    public void deleteAll() {
        log.debug("Удаление всех событий");
        jdbcTemplate.update("DELETE FROM events");
    }

    private Event mapRowToEvent(ResultSet rs, int rowNum) throws SQLException {
        return Event.builder()
                .eventId(rs.getLong("event_id"))
                .userId(rs.getLong("user_id"))
                .eventType(Event.EventType.valueOf(rs.getString("event_type")))
                .operation(Event.Operation.valueOf(rs.getString("operation")))
                .entityId(rs.getLong("entity_id"))
                .timestamp(rs.getTimestamp("timestamp").getTime())
                .build();
    }
}