package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.List;

public interface FilmStorage {

    Collection<Film> findAll();

    List<Film> findAllBy(Integer directorId, String sortBy);

    Film create(Film film);

    Film update(Film film);

    Optional<Film> getById(Long id);

    void deleteFilm(Long id);

    void addLike(Long filmId, Long userId);

    void deleteLike(Long filmId, Long userId);

    List<Film> getPopular(int count, Integer genreId, Integer year);

    List<Film> search(String query, String by);

    List<Film> getCommonFilms(Long userId, Long friendId);

    Map<Long, List<Long>> getAllLikes();

    List<Film> getFilmsNotLikedByUser(Long userId);

    List<Film> getFilmsWithFilter(Map<String, String> params);
}