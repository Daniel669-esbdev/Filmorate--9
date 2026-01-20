package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.List;

public interface FilmStorage {

    Collection<Film> findAll();

    Film create(Film film);

    Film update(Film film);

    Optional<Film> getById(Long id);

    void addLike(Long filmId, Long userId);

    void deleteLike(Long filmId, Long userId);

    List<Film> getPopular(int count);

    // Новый метод для получения лайков всех пользователей
    Map<Long, List<Long>> getAllLikes();

    // Новый метод для получения фильмов, которые не лайкнул пользователь
    List<Film> getFilmsNotLikedByUser(Long userId);
}