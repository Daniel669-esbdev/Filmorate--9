package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Review create(Review review) {
        String sql = """
            INSERT INTO reviews (film_id, user_id, content, created_at, updated_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, review.getFilmId());
            ps.setLong(2, review.getUserId());
            ps.setString(3, review.getContent());
            return ps;
        }, keyHolder);

        review.setId(keyHolder.getKey().longValue());
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = """
            UPDATE reviews
            SET content = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        int updated = jdbcTemplate.update(sql,
                review.getContent(),
                review.getId());

        if (updated == 0) {
            throw new NotFoundException("Отзыв с id = " + review.getId() + " не найден");
        }

        review.setUpdatedAt(LocalDateTime.now());
        return review;
    }

    @Override
    public boolean delete(Long reviewId) {
        String sql = "DELETE FROM reviews WHERE id = ?";
        return jdbcTemplate.update(sql, reviewId) > 0;
    }

    @Override
    public Optional<Review> findById(Long id) {
        String sql = """
            SELECT id, film_id, user_id, content, created_at, updated_at
            FROM reviews WHERE id = ?
            """;

        List<Review> reviews = jdbcTemplate.query(sql, this::mapRowToReview, id);
        return reviews.isEmpty() ? Optional.empty() : Optional.of(reviews.get(0));
    }

    @Override
    public List<Review> findByFilmId(Long filmId, Integer count) {
        String sql = """
            SELECT id, film_id, user_id, content, created_at, updated_at
            FROM reviews
            WHERE film_id = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, this::mapRowToReview, filmId, count);
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        Review review = new Review();
        review.setId(rs.getLong("id"));
        review.setFilmId(rs.getLong("film_id"));
        review.setUserId(rs.getLong("user_id"));
        review.setContent(rs.getString("content"));
        review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        review.setUpdatedAt(rs.getTimestamp("updated_at") != null
                ? rs.getTimestamp("updated_at").toLocalDateTime()
                : null);
        return review;
    }
}