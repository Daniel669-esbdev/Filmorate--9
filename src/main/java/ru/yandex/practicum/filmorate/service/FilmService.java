package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        if (filmStorage.getById(film.getId()) == null) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        return filmStorage.update(film);
    }

    public Film getById(Long id) {
        Film film = filmStorage.getById(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        return film;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = getById(filmId);
        if (userStorage.getById(userId) == null) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }
        film.getLikes().add(userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        Film film = getById(filmId);
        if (userStorage.getById(userId) == null) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (film.getLikes() != null) {
            film.getLikes().remove(userId);
        }
    }

    public List<Film> getPopular(int count) {
        return filmStorage.findAll().stream()
                .sorted((f1, f2) -> {
                    int likes1 = f1.getLikes() == null ? 0 : f1.getLikes().size();
                    int likes2 = f2.getLikes() == null ? 0 : f2.getLikes().size();
                    return Integer.compare(likes2, likes1);
                })
                .limit(count)
                .collect(Collectors.toList());
    }
}