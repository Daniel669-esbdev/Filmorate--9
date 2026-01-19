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
        if (review == null) {
            throw new ValidationException("Отзыв не может быть null");
        }
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ValidationException("Содержимое отзыва не может быть пустым");
        }
        if (review.getIsPositive() == null) {
            throw new ValidationException("isPositive обязателен");
        }
        if (review.getFilmId() == null || review.getFilmId() <= 0) {
            throw new ValidationException("filmId обязателен и должен быть положительным");
        }
        if (review.getUserId() == null || review.getUserId() <= 0) {
            throw new ValidationException("userId обязателен и должен быть положительным");
        }

        Integer filmCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM films WHERE id = ?",
                Integer.class,
                review.getFilmId()
        );
        if (filmCount == null || filmCount == 0) {
            throw new ValidationException("Фильм с id = " + review.getFilmId() + " не найден");
        }

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                review.getUserId()
        );
        if (userCount == null || userCount == 0) {
            throw new ValidationException("Пользователь с id = " + review.getUserId() + " не найден");
        }

        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE film_id = ? AND user_id = ?",
                Integer.class,
                review.getFilmId(),
                review.getUserId()
        );
        if (existing != null && existing > 0) {
            throw new ValidationException("Пользователь уже оставил отзыв на этот фильм");
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
        if (filmId == null) {
            throw new ValidationException("ID фильма не может быть null");
        }

        Integer filmCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM films WHERE id = ?",
                Integer.class,
                filmId
        );
        if (filmCount == null || filmCount == 0) {
            throw new ValidationException("Фильм с id = " + filmId + " не найден");
        }

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

    private void validateReviewAndUser(Long reviewId, Long userId) {
        if (reviewId == null || userId == null) {
            throw new ValidationException("ID отзыва или пользователя не может быть null");
        }

        reviewStorage.findById(reviewId)
                .orElseThrow(() -> new NotFoundException(
                        "Отзыв с id = " + reviewId + " не найден"));

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Integer.class,
                userId
        );
        if (userCount == null || userCount == 0) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }
}