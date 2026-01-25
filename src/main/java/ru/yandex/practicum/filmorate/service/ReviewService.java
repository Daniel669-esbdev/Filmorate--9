package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;

    public Review create(Review review) {
        log.info("Создание отзыва: filmId={}, userId={}", review.getFilmId(), review.getUserId());
        validateReviewFields(review);

        checkUserExists(review.getUserId());
        checkFilmExists(review.getFilmId());

        return reviewStorage.create(review);
    }

    public Review update(Review review) {
        log.info("Обновление отзыва с id = {}", review.getId());
        validateReviewFields(review);

        getReviewOrThrow(review.getId());

        return reviewStorage.update(review);
    }

    public void delete(Long reviewId) {
        log.info("Удаление отзыва с id = {}", reviewId);
        if (reviewId == null) {
            throw new ValidationException("ID отзыва не может быть null");
        }
        if (!reviewStorage.delete(reviewId)) {
            throw new NotFoundException("Отзыв с id = " + reviewId + " не найден");
        }
    }

    public Review getById(Long id) {
        log.info("Получение отзыва по id = {}", id);
        return getReviewOrThrow(id);
    }

    public List<Review> getByFilm(Long filmId, Integer count) {
        log.info("Запрос отзывов для фильма id = {}, лимит = {}", filmId, count);
        if (filmId != null && filmId != 0) {
            checkFilmExists(filmId);
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

    private Review getReviewOrThrow(Long id) {
        if (id == null) {
            throw new ValidationException("ID отзыва не может быть null");
        }
        return reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id = " + id + " не найден"));
    }

    private void checkUserExists(Long userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
    }

    private void checkFilmExists(Long filmId) {
        filmStorage.getById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + filmId + " не найден"));
    }

    private void validateReviewAndUser(Long reviewId, Long userId) {
        getReviewOrThrow(reviewId);
        checkUserExists(userId);
    }

    private void validateReviewFields(Review review) {
        if (review == null) {
            throw new ValidationException("Отзыв не может быть null");
        }
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ValidationException("Содержимое отзыва не может быть пустым");
        }
        if (review.getIsPositive() == null) {
            throw new ValidationException("Статус отзыва (isPositive) обязателен");
        }
        if (review.getFilmId() == null) {
            throw new ValidationException("filmId обязателен");
        }
        if (review.getUserId() == null) {
            throw new ValidationException("userId обязателен");
        }
    }
}