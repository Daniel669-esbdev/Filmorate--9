package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedDbStorage implements FeedStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void addEvent(Long userId, String eventType, String operation, Long entityId) {
        String sql = "INSERT INTO feed (timestamp, user_id, event_type, operation, entity_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, System.currentTimeMillis(), userId, eventType, operation, entityId);
    }

    @Override
    public List<Event> getFeed(Long userId) {
        String sql = "SELECT * FROM feed WHERE user_id = ? ORDER BY timestamp ASC";
        return jdbcTemplate.query(sql, this::mapRowToEvent, userId);
    }

    private Event mapRowToEvent(ResultSet rs, int rowNum) throws SQLException {
        return Event.builder()
                .eventId(rs.getLong("event_id"))
                .timestamp(rs.getLong("timestamp"))
                .userId(rs.getLong("user_id"))
                .eventType(rs.getString("event_type"))
                .operation(rs.getString("operation"))
                .entityId(rs.getLong("entity_id"))
                .build();
    }
}