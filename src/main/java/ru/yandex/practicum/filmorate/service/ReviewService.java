package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final JdbcTemplate jdbcTemplate;

    public Review create(Review review) {
        validateReviewFields(review);

        if (!isFilmExists(review.getFilmId())) {
            throw new NotFoundException("Фильм с id = " + review.getFilmId() + " не найден");
        }

        if (!isUserExists(review.getUserId())) {
            throw new NotFoundException("Пользователь с id = " + review.getUserId() + " не найден");
        }

        return reviewStorage.create(review);
    }

    public Review update(ReviewUpdate reviewUpdate) {
        Review existing = reviewStorage.findById(reviewUpdate.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Отзыв с id = " + reviewUpdate.getId() + " не найден"));

        existing.setContent(reviewUpdate.getContent());
        existing.setIsPositive(reviewUpdate.getIsPositive());

        return reviewStorage.update(existing);
    }

    public void delete(Long reviewId) {
        if (reviewId == null) {
            throw new ValidationException("ID отзыва не может быть null");
        }
        if (!reviewStorage.delete(reviewId)) {
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден");
        }
    }

    public Review getById(Long id) {
        if (id == null) {
            throw new ValidationException("ID отзыва не может быть null");
        }
        return reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Отзыв с id = " + id + " не найден"));
    }

    public List<Review> getByFilm(Long filmId, Integer count) {
        if (filmId != null && filmId != 0 && !isFilmExists(filmId)) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
        return reviewStorage.findByFilmId(filmId, count != null ? count : 10);
    }

    public void addLike(Long reviewId, Long userId) {
        validateReviewAndUser(reviewId, userId);
        reviewStorage.addLike(reviewId, userId);
    }

    public void removeLike(Long reviewId, Long userId) {
        validateReviewAndUser(reviewId, userId);
        reviewStorage.removeLike(reviewId, userId);
    }

    public void addDislike(Long reviewId, Long userId) {
        validateReviewAndUser(reviewId, userId);
        reviewStorage.addDislike(reviewId, userId);
    }

    public void removeDislike(Long reviewId, Long userId) {
        validateReviewAndUser(reviewId, userId);
        reviewStorage.removeDislike(reviewId, userId);
    }

    private void validateReviewAndUser(Long reviewId, Long userId) {
        if (reviewId == null || userId == null) {
            throw new ValidationException("ID отзыва или пользователя не может быть null");
        }

        if (reviewStorage.findById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден");
        }

        if (!isUserExists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }

    private void validateReviewFields(Review review) {
        if (review == null) {
            throw new ValidationException("Отзыв не может быть null");
        }
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ValidationException("Содержимое отзыва не может быть пустым");
        }
        if (review.getIsPositive() == null) {
            throw new ValidationException("isPositive обязателен");
        }
        if (review.getFilmId() == null) {
            throw new ValidationException("filmId обязателен");
        }
        if (review.getUserId() == null) {
            throw new ValidationException("userId обязателен");
        }
    }

    private boolean isFilmExists(Long filmId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM films WHERE id = ?", Integer.class, filmId);
        return count != null && count > 0;
    }

    private boolean isUserExists(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId);
        return count != null && count > 0;
    }
}