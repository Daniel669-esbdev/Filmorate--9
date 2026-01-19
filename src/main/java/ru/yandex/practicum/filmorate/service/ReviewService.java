package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    public Review create(Review review) {
        validate(review);
        if (review.getIsPositive() == null) {
            throw new ValidationException("Поле isPositive обязательно");
        }
        checkUserDidNotReviewYet(review.getFilmId(), review.getUserId());
        return reviewStorage.create(review);
    }

    public Review update(Review review) {
        validate(review);
        Review existing = reviewStorage.findById(review.getId())
                .orElseThrow(() -> new NotFoundException("Отзыв не найден"));

        if (review.getIsPositive() == null) {
            throw new ValidationException("Поле isPositive обязательно");
        }

        return reviewStorage.update(review);
    }

    public void delete(Long reviewId) {
        if (!reviewStorage.delete(reviewId)) {
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден");
        }
    }

    public Review getById(Long id) {
        return reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id = " + id + " не найден"));
    }

    public List<Review> getByFilm(Long filmId, Integer count) {
        filmStorage.getById(filmId);
        return reviewStorage.findByFilmId(filmId, count != null ? count : 10);
    }

    public Review addLike(Long reviewId, Long userId) {
        validateReviewAndUser(reviewId, userId);
        reviewStorage.addLike(reviewId, userId);
        return getById(reviewId);
    }

    public Review removeLike(Long reviewId, Long userId) {
        validateReviewAndUser(reviewId, userId);
        reviewStorage.removeLike(reviewId, userId);
        return getById(reviewId);
    }

    public Review addDislike(Long reviewId, Long userId) {
        validateReviewAndUser(reviewId, userId);
        reviewStorage.addDislike(reviewId, userId);
        return getById(reviewId);
    }

    public Review removeDislike(Long reviewId, Long userId) {
        validateReviewAndUser(reviewId, userId);
        reviewStorage.removeDislike(reviewId, userId);
        return getById(reviewId);
    }

    private void validate(Review review) {
        if (review.getContent() == null || review.getContent().trim().isEmpty()) {
            throw new ValidationException("Содержимое отзыва не может быть пустым");
        }
    }

    private void checkUserDidNotReviewYet(Long filmId, Long userId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId, userId);
        if (count != null && count > 0) {
            throw new ValidationException("Вы уже оставляли отзыв к этому фильму");
        }
    }

    private void validateReviewAndUser(Long reviewId, Long userId) {
        if (reviewStorage.findById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв не найден");
        }
        if (userStorage.getById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь не найден");
        }
    }
}