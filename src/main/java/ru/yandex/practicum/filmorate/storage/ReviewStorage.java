package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {
    Review create(Review review);
    Review update(Review review);
    boolean delete(Long reviewId);
    Optional<Review> findById(Long id);
    List<Review> findByFilmId(Long filmId, Integer count);
}