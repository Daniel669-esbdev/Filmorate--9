package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;
    private final FeedStorage feedStorage; // Возвращаем для ленты событий

    @Override
    public Review create(Review review) {
        validateUserExists(review.getUserId());
        validateFilmExists(review.getFilmId());

        String sql = "INSERT INTO reviews (film_id, user_id, content, is_positive, useful, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 0, ?, ?)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, review.getFilmId());
            ps.setLong(2, review.getUserId());
            ps.setString(3, review.getContent());
            ps.setBoolean(4, review.getIsPositive());
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setTimestamp(6, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);

        review.setId(keyHolder.getKey().longValue());
        review.setUseful(0);

        // Регистрация события в ленте (Feature: Feed)
        feedStorage.addEvent(review.getUserId(), "REVIEW", "ADD", review.getId());

        return review;
    }

    @Override
    public Review update(Review review) {
        validateReviewExists(review.getId());
        String sql = "UPDATE reviews SET content = ?, is_positive = ?, updated_at = ? WHERE id = ?";
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(sql,
                review.getContent(),
                review.getIsPositive(),
                Timestamp.valueOf(now),
                review.getId());

        Review updatedReview = findById(review.getId()).get();

        // Регистрация события обновления в ленте
        feedStorage.addEvent(updatedReview.getUserId(), "REVIEW", "UPDATE", updatedReview.getId());

        return updatedReview;
    }

    @Override
    public boolean delete(Long reviewId) {
        Optional<Review> review = findById(reviewId);
        if (review.isPresent()) {
            String sql = "DELETE FROM reviews WHERE id = ?";
            boolean deleted = jdbcTemplate.update(sql, reviewId) > 0;
            if (deleted) {
                feedStorage.addEvent(review.get().getUserId(), "REVIEW", "REMOVE", reviewId);
            }
            return deleted;
        }
        return false;
    }

    @Override
    public Optional<Review> findById(Long id) {
        String sql = "SELECT * FROM reviews WHERE id = ?";
        List<Review> reviews = jdbcTemplate.query(sql, this::mapRowToReview, id);
        return reviews.stream().findFirst();
    }

    @Override
    public List<Review> findByFilmId(Long filmId, Integer count) {
        String sql;
        if (filmId == null) {
            sql = "SELECT * FROM reviews ORDER BY useful DESC, id ASC LIMIT ?";
            return jdbcTemplate.query(sql, this::mapRowToReview, count);
        } else {
            sql = "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC, id ASC LIMIT ?";
            return jdbcTemplate.query(sql, this::mapRowToReview, filmId, count);
        }
    }

    @Override
    public void addLike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        validateUserExists(userId);
        updateVote(reviewId, userId, true);
    }

    @Override
    public void addDislike(Long reviewId, Long userId) {
        validateReviewExists(reviewId);
        validateUserExists(userId);
        updateVote(reviewId, userId, false);
    }

    @Override
    public void removeLike(Long reviewId, Long userId) {
        deleteVote(reviewId, userId);
    }

    @Override
    public void removeDislike(Long reviewId, Long userId) {
        deleteVote(reviewId, userId);
    }

    private void updateVote(Long reviewId, Long userId, boolean isLike) {
        jdbcTemplate.update("DELETE FROM review_votes WHERE review_id = ? AND user_id = ?", reviewId, userId);
        jdbcTemplate.update("INSERT INTO review_votes (review_id, user_id, is_like) VALUES (?, ?, ?)",
                reviewId, userId, isLike);
        recalculateUseful(reviewId);
    }

    private void deleteVote(Long reviewId, Long userId) {
        jdbcTemplate.update("DELETE FROM review_votes WHERE review_id = ? AND user_id = ?", reviewId, userId);
        recalculateUseful(reviewId);
    }

    private void recalculateUseful(Long reviewId) {
        String sql = "UPDATE reviews SET useful = (SELECT " +
                "COALESCE(COUNT(CASE WHEN is_like = true THEN 1 END), 0) - " +
                "COALESCE(COUNT(CASE WHEN is_like = false THEN 1 END), 0) " +
                "FROM review_votes WHERE review_id = ?) WHERE id = ?";
        jdbcTemplate.update(sql, reviewId, reviewId);
    }

    private void validateUserExists(Long userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId);
        if (count == null || count == 0) throw new NotFoundException("Пользователь " + userId + " не найден");
    }

    private void validateFilmExists(Long filmId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM films WHERE id = ?", Integer.class, filmId);
        if (count == null || count == 0) throw new NotFoundException("Фильм " + filmId + " не найден");
    }

    private void validateReviewExists(Long reviewId) {
        if (findById(reviewId).isEmpty()) throw new NotFoundException("Отзыв " + reviewId + " не найден");
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        Review review = new Review();
        review.setId(rs.getLong("id"));
        review.setFilmId(rs.getLong("film_id"));
        review.setUserId(rs.getLong("user_id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUseful(rs.getInt("useful"));
        return review;
    }
}