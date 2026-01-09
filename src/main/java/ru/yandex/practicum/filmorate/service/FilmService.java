package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.NotFoundException;
import ru.yandex.practicum.filmorate.controller.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public FilmService(FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateFilm(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        if (filmStorage.getById(film.getId()) == null) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        validateFilm(film);
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
        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }
        film.getLikes().add(userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        Film film = getById(filmId);
        if (film.getLikes() != null) {
            film.getLikes().remove(userId);
        }
    }

    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        Stream<Film> films = filmStorage.findAll().stream();

        if (genreId != null) {
            films = films.filter(film ->
                    film.getGenres() != null &&
                            film.getGenres().stream().anyMatch(g -> g.getId() == genreId)
            );
        }

        if (year != null) {
            films = films.filter(film ->
                    film.getReleaseDate() != null &&
                            film.getReleaseDate().getYear() == year
            );
        }

        return films
                .sorted((f1, f2) -> {
                    int likes1 = f1.getLikes() == null ? 0 : f1.getLikes().size();
                    int likes2 = f2.getLikes() == null ? 0 : f2.getLikes().size();
                    return Integer.compare(likes2, likes1);
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    public List<Film> getPopular(int count) {
        return getPopular(count, null, null);
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }
}