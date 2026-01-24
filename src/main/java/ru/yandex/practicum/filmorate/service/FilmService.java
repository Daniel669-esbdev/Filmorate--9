package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final DirectorStorage directorStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;
    private final FeedStorage feedStorage;

    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public FilmService(
            FilmStorage filmStorage,
            DirectorStorage directorStorage,
            UserStorage userStorage,
            MpaStorage mpaStorage,
            GenreStorage genreStorage,
            FeedStorage feedStorage
    ) {
        this.filmStorage = filmStorage;
        this.directorStorage = directorStorage;
        this.userStorage = userStorage;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
        this.feedStorage = feedStorage;
        log.info("FilmService инициализирован");
    }

    public Collection<Film> findAll() {
        log.info("Запрос на получение всех фильмов");
        Collection<Film> films = filmStorage.findAll();
        log.info("Успешно получено {} фильмов", films.size());
        return films;
    }

    public List<Film> getFilmsByDirector(Integer directorId, String sortBy) {
        log.info("Запрос фильмов режиссера id={} с сортировкой по {}", directorId, sortBy);
        directorStorage.getDirectorById(directorId);
        return filmStorage.findAllBy(directorId, sortBy);
    }

    public Film create(Film film) {
        log.info("Начало создания фильма: name={}, releaseDate={}", film.getName(), film.getReleaseDate());
        validateFilm(film);
        Film created = filmStorage.create(film);
        log.info("Фильм успешно создан, id={}, name={}", created.getId(), created.getName());
        return created;
    }

    public Film update(Film film) {
        log.info("Начало обновления фильма id={}, name={}", film.getId(), film.getName());
        getById(film.getId());
        validateFilm(film);
        Film updated = filmStorage.update(film);
        log.info("Фильм успешно обновлён, id={}, name={}", updated.getId(), updated.getName());
        return updated;
    }

    public Film getById(Long id) {
        log.info("Запрос фильма по id={}", id);
        return filmStorage.getById(id)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Фильм с id=%d не найден", id);
                    log.error(errorMsg);
                    return new NotFoundException(errorMsg);
                });
    }

    public void deleteFilm(Long id) {
        log.debug("Запрос на удаление фильма по id = {}", id);
        filmStorage.deleteFilm(id);
        log.debug("Фильм с id = {} успешно удалён", id);
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Начало добавления лайка: filmId={}, userId={}", filmId, userId);
        getById(filmId);
        validateUserExists(userId);
        filmStorage.addLike(filmId, userId);
        feedStorage.addEvent(userId, "LIKE", "ADD", filmId);
        log.info("Лайк успешно добавлен: filmId={}, userId={}", filmId, userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        log.info("Начало удаления лайка: filmId={}, userId={}", filmId, userId);
        getById(filmId);
        validateUserExists(userId);
        filmStorage.deleteLike(filmId, userId);
        feedStorage.addEvent(userId, "LIKE", "REMOVE", filmId);
        log.info("Лайк успешно удалён: filmId={}, userId={}", filmId, userId);
    }

    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        log.info("Запрос популярных фильмов: count={}, genreId={}, year={}", count, genreId, year);
        if (count <= 0) {
            String errorMsg = "Параметр count должен быть больше 0";
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        List<Film> films = filmStorage.getPopular(count, genreId, year);
        log.info("Успешно получено {} популярных фильмов", films.size());
        return films;
    }

    public List<Film> search(String query, String by) {
        log.info("Поиск фильмов по запросу: query={}, by={}", query, by);
        return filmStorage.search(query, by);
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        log.info("Запрос общих фильмов для пользователей userId={} и friendId={}", userId, friendId);
        validateUserExists(userId);
        validateUserExists(friendId);

        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя искать общие фильмы с самим собой");
        }

        return filmStorage.getCommonFilms(userId, friendId);
    }

    private void validateFilm(Film film) {
        log.debug("Начало валидации фильма: name={}, releaseDate={}", film.getName(), film.getReleaseDate());

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            String errorMsg = "Дата релиза — не раньше 28 декабря 1895 года";
            log.error(errorMsg);
            throw new ValidationException(errorMsg);
        }

        if (film.getMpa() != null && film.getMpa().getId() != null) {
            mpaStorage.findById(film.getMpa().getId())
                    .orElseThrow(() -> {
                        String errorMsg = String.format("Рейтинг MPA с id=%d не найден", film.getMpa().getId());
                        log.error(errorMsg);
                        return new NotFoundException(errorMsg);
                    });
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (genre.getId() != null) {
                    genreStorage.findById(genre.getId())
                            .orElseThrow(() -> {
                                String errorMsg = String.format("Жанр с id=%d не найден", genre.getId());
                                log.error(errorMsg);
                                return new NotFoundException(errorMsg);
                            });
                }
            }
        }

        log.debug("Валидация фильма прошла успешно");
    }

    private void validateUserExists(Long userId) {
        log.debug("Проверка существования пользователя id={}", userId);
        userStorage.getById(userId)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Пользователь с id=%d не найден", userId);
                    log.error(errorMsg);
                    return new NotFoundException(errorMsg);
                });
        log.debug("Пользователь id={} существует", userId);
    }
}