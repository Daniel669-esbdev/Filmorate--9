package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface FilmStorage {

    Collection<Film> findAll();

    List<Film> findAllBy(Integer directorId, String sortBy);

    Film create(Film film);

    Film update(Film film);

    Optional<Film> getById(Long id);

    void addLike(Long filmId, Long userId);

    void deleteLike(Long filmId, Long userId);

    List<Film> getPopular(int count, Integer genreId, Integer year);

    List<Film> search(String query, String by);
}