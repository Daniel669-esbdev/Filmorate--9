package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public FilmService(
            FilmStorage filmStorage,
            UserStorage userStorage,
            MpaStorage mpaStorage,
            GenreStorage genreStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
    }

    public Collection<Film> findAll() {
        log.debug("Запрос на получение всех фильмов");
        Collection<Film> films = filmStorage.findAll();
        log.debug("Получено фильмов: {}", films.size());
        return films;
    }

    public List<Film> getFilmsByDirector(Integer directorId, String sortBy) {
        log.info("Запрос фильмов режиссера id={} с сортировкой по {}", directorId, sortBy);
        return filmStorage.findAllBy(directorId, sortBy);
    }

    public Film create(Film film) {
        log.info("Создание фильма: name={}, releaseDate={}", film.getName(), film.getReleaseDate());
        validateFilm(film);
        Film created = filmStorage.create(film);
        log.info("Фильм создан успешно, id={}", created.getId());
        return created;
    }

    public Film update(Film film) {
        log.info("Обновление фильма id={}", film.getId());
        if (filmStorage.getById(film.getId()).isEmpty()) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        validateFilm(film);
        Film updated = filmStorage.update(film);
        log.info("Фильм id={} успешно обновлён", updated.getId());
        return updated;
    }

    public Film getById(Long id) {
        log.debug("Запрос фильма по id={}", id);
        Film film = filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
        log.debug("Фильм найден: name={}", film.getName());
        return film;
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Добавление лайка: filmId={}, userId={}", filmId, userId);
        getById(filmId);
        validateUserExists(userId);
        filmStorage.addLike(filmId, userId);
        log.info("Лайк добавлен успешно: filmId={}, userId={}", filmId, userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        log.info("Удаление лайка: filmId={}, userId={}", filmId, userId);
        getById(filmId);
        validateUserExists(userId);
        filmStorage.deleteLike(filmId, userId);
        log.info("Лайк удалён: filmId={}, userId={}", filmId, userId);
    }

    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        log.info("Запрос популярных фильмов: count={}", count);
        if (count <= 0) {
            throw new IllegalArgumentException("Параметр count должен быть больше 0");
        }
        List<Film> films = filmStorage.getPopular(count, genreId, year);
        log.debug("Получено популярных фильмов: {}", films.size());
        return films;
    }

    public List<Film> search(String query, String by) {
        log.info("Поиск фильмов по запросу: query={}, by={}", query, by);
        return filmStorage.search(query, by);
    }

    private void validateFilm(Film film) {
        log.debug("Валидация фильма: name={}, releaseDate={}", film.getName(), film.getReleaseDate());

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза — не раньше 28 декабря 1895 года");
        }

        if (film.getMpa() != null && film.getMpa().getId() != null) {
            if (mpaStorage.findById(film.getMpa().getId()).isEmpty()) {
                throw new NotFoundException("Рейтинг MPA с id=" + film.getMpa().getId() + " не найден");
            }
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (genre.getId() != null && genreStorage.findById(genre.getId()).isEmpty()) {
                    throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
                }
            }
        }

        log.debug("Валидация фильма прошла успешно");
    }

    private void validateUserExists(Long userId) {
        if (userStorage.getById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}