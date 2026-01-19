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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
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

        if (filmStorage.getById(review.getFilmId()) == null) {
            throw new ValidationException("Фильм с id = " + review.getFilmId() + " не найден");
        }
        if (userStorage.getById(review.getUserId()).isEmpty()) {
            throw new ValidationException("Пользователь с id = " + review.getUserId() + " не найден");
        }

        checkUserDidNotReviewYet(review.getFilmId(), review.getUserId());

        return reviewStorage.create(review);
    }

    public Review update(ReviewUpdate dto) {
        Review review = reviewStorage.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Отзыв с id = " + dto.getId() + " не найден"));

        review.setContent(dto.getContent());
        review.setIsPositive(dto.getIsPositive());

        return reviewStorage.update(review);
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

    private void checkUserDidNotReviewYet(Long filmId, Long userId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId, userId);
        if (count != null && count > 0) {
            throw new ValidationException("Пользователь уже оставил отзыв на этот фильм");
        }
    }

    private void validateReviewAndUser(Long reviewId, Long userId) {
        if (reviewId == null || userId == null) {
            throw new ValidationException("ID отзыва или пользователя не может быть null");
        }
        reviewStorage.findById(reviewId)
                .orElseThrow(() -> new NotFoundException(
                        "Отзыв с id = " + reviewId + " не найден"));

        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id = " + userId + " не найден"));
    }
}