package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.NotFoundException;
import ru.yandex.practicum.filmorate.controller.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Collection<Film> findAll() {
        log.info("Fetching all films");
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        log.info("Creating film: {}", film);
        validateFilm(film);
        Film created = filmStorage.create(film);
        log.info("Film created with id: {}", created.getId());
        return created;
    }

    public Film update(Film film) {
        log.info("Updating film id: {}", film.getId());
        if (filmStorage.getById(film.getId()).isEmpty()) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        validateFilm(film);
        Film updated = filmStorage.update(film);
        log.info("Film updated successfully");
        return updated;
    }

    public Film getById(Long id) {
        log.info("Getting film by id: {}", id);
        return filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Adding like from user {} to film {}", userId, filmId);
        Film film = getById(filmId);
        if (userStorage.getById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }
        film.getLikes().add(userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        log.info("Removing like from user {} to film {}", userId, filmId);
        Film film = getById(filmId);
        if (userStorage.getById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (film.getLikes() != null) {
            film.getLikes().remove(userId);
        }
    }

    public List<Film> getPopular(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than 0");
        }
        log.info("Fetching popular films, count: {}", count);
        return filmStorage.findAll().stream()
                .sorted((f1, f2) -> {
                    int likes1 = f1.getLikes() == null ? 0 : f1.getLikes().size();
                    int likes2 = f2.getLikes() == null ? 0 : f2.getLikes().size();
                    return Integer.compare(likes2, likes1);
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }
}