package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDbStorage implements EventStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addEvent(Event event) {
        log.info("Добавление события: userId={}, eventType={}, operation={}, entityId={}",
                event.getUserId(), event.getEventType(), event.getOperation(), event.getEntityId());

        String sql = """
                INSERT INTO events (user_id, entity_id, event_type, operation, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"event_id"});
            stmt.setLong(1, event.getUserId());
            stmt.setLong(2, event.getEntityId());
            stmt.setString(3, event.getEventType().toString());
            stmt.setString(4, event.getOperation().toString());
            stmt.setTimestamp(5, Timestamp.valueOf(event.getTimestamp()));
            return stmt;
        }, keyHolder);

        event.setEventId(keyHolder.getKey().longValue());
        log.info("Событие добавлено с id: {}", event.getEventId());
    }

    @Override
    public List<Event> getEventsByUserId(Long userId) {
        log.info("Получение событий для пользователя с id={}", userId);

        String sql = """
                SELECT * FROM events 
                WHERE user_id = ? 
                ORDER BY timestamp DESC
                """;

        List<Event> events = jdbcTemplate.query(sql, this::mapRowToEvent, userId);
        log.info("Найдено событий: {} для пользователя с id={}", events.size(), userId);
        return events;
    }

    private Event mapRowToEvent(ResultSet rs, int rowNum) throws SQLException {
        Event event = new Event();
        event.setEventId(rs.getLong("event_id"));
        event.setUserId(rs.getLong("user_id"));
        event.setEntityId(rs.getLong("entity_id"));
        event.setEventType(EventType.valueOf(rs.getString("event_type")));
        event.setOperation(Operation.valueOf(rs.getString("operation")));
        event.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        return event;
    }
}