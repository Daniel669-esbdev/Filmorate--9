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
            INSERT INTO reviews (film_id, user_id, content, is_positive, useful, created_at, updated_at)
            VALUES (?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, review.getFilmId());
            ps.setLong(2, review.getUserId());
            ps.setString(3, review.getContent());
            ps.setBoolean(4, review.getIsPositive());
            return ps;
        }, keyHolder);

        long newId = keyHolder.getKey().longValue();

        review.setId(newId);
        review.setUseful(0);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = """
            UPDATE reviews
            SET content = ?, is_positive = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        int updated = jdbcTemplate.update(sql,
                review.getContent(),
                review.getIsPositive(),
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
            SELECT id, film_id, user_id, content, is_positive, useful, created_at, updated_at
            FROM reviews WHERE id = ?
            """;

        List<Review> reviews = jdbcTemplate.query(sql, this::mapRowToReview, id);
        return reviews.isEmpty() ? Optional.empty() : Optional.of(reviews.get(0));
    }

    @Override
    public List<Review> findByFilmId(Long filmId, Integer count) {
        String sql = """
            SELECT id, film_id, user_id, content, is_positive, useful, created_at, updated_at
            FROM reviews
            WHERE film_id = ?
            ORDER BY useful DESC, id DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, this::mapRowToReview, filmId, count);
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        validateUserExists(userId);

        jdbcTemplate.update("""
                DELETE FROM review_votes
                WHERE review_id = ? AND user_id = ?
                """, reviewId, userId);

        jdbcTemplate.update("""
                INSERT INTO review_votes (review_id, user_id, is_like)
                VALUES (?, ?, true)
                """, reviewId, userId);

        updateUseful(reviewId);
    }

    @Override
    public void removeLike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        validateUserExists(userId);

        jdbcTemplate.update("""
                DELETE FROM review_votes
                WHERE review_id = ? AND user_id = ? AND is_like = true
                """, reviewId, userId);

        updateUseful(reviewId);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        validateUserExists(userId);

        jdbcTemplate.update("""
                DELETE FROM review_votes
                WHERE review_id = ? AND user_id = ?
                """, reviewId, userId);

        jdbcTemplate.update("""
                INSERT INTO review_votes (review_id, user_id, is_like)
                VALUES (?, ?, false)
                """, reviewId, userId);

        updateUseful(reviewId);
    }

    @Override
    public void removeDislike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        validateUserExists(userId);

        jdbcTemplate.update("""
                DELETE FROM review_votes
                WHERE review_id = ? AND user_id = ? AND is_like = false
                """, reviewId, userId);

        updateUseful(reviewId);
    }

    private void updateUseful(Long reviewId) {
        jdbcTemplate.update("""
            UPDATE reviews
            SET useful = (
                SELECT COALESCE(SUM(CASE WHEN is_like THEN 1 ELSE -1 END), 0)
                FROM review_votes
                WHERE review_id = ?
            )
            WHERE id = ?
            """, reviewId, reviewId);
    }

    private void validateUserExists(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId не может быть null");
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId);
        if (count == null || count == 0) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }

    private void validateFilmExists(Long filmId) {
        if (filmId == null) {
            throw new IllegalArgumentException("filmId не может быть null");
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM films WHERE id = ?", Integer.class, filmId);
        if (count == null || count == 0) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
    }

    private void validateReviewExists(Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("reviewId не может быть null");
        }
        if (findById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден");
        }
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        Review review = new Review();
        review.setId(rs.getLong("id"));
        review.setFilmId(rs.getLong("film_id"));
        review.setUserId(rs.getLong("user_id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUseful(rs.getInt("useful"));
        review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null) {
            review.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return review;
    }
}