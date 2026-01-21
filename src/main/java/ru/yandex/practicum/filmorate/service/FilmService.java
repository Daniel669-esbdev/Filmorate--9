package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
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
    private final EventService eventService;

    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public FilmService(
            FilmStorage filmStorage,
            UserStorage userStorage,
            MpaStorage mpaStorage,
            GenreStorage genreStorage,
            EventService eventService
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
        this.eventService = eventService;
        log.info("FilmService инициализирован");
    }

    public Collection<Film> findAll() {
        log.info("Запрос на получение всех фильмов");
        try {
            Collection<Film> films = filmStorage.findAll();
            log.info("Успешно получено {} фильмов", films.size());
            return films;
        } catch (Exception e) {
            log.error("Ошибка при получении всех фильмов: {}", e.getMessage());
            throw new RuntimeException("Не удалось получить список фильмов", e);
        }
    }

    public Film create(Film film) {
        log.info("Начало создания фильма: name={}, releaseDate={}", film.getName(), film.getReleaseDate());
        try {
            validateFilm(film);
            Film created = filmStorage.create(film);
            log.info("Фильм успешно создан, id={}, name={}", created.getId(), created.getName());
            return created;
        } catch (ValidationException e) {
            log.error("Ошибка валидации при создании фильма: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании фильма: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать фильм", e);
        }
    }

    public Film update(Film film) {
        log.info("Начало обновления фильма id={}, name={}", film.getId(), film.getName());
        try {
            if (filmStorage.getById(film.getId()).isEmpty()) {
                String errorMsg = String.format("Фильм с id=%d не найден", film.getId());
                log.error(errorMsg);
                throw new NotFoundException(errorMsg);
            }
            validateFilm(film);
            Film updated = filmStorage.update(film);
            log.info("Фильм успешно обновлён, id={}, name={}", updated.getId(), updated.getName());
            return updated;
        } catch (NotFoundException | ValidationException e) {
            log.error("Ошибка при обновлении фильма: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обновлении фильма id={}: {}", film.getId(), e.getMessage());
            throw new RuntimeException("Не удалось обновить фильм", e);
        }
    }

    public Film getById(Long id) {
        log.info("Запрос фильма по id={}", id);
        try {
            Film film = filmStorage.getById(id)
                    .orElseThrow(() -> {
                        String errorMsg = String.format("Фильм с id=%d не найден", id);
                        log.error(errorMsg);
                        return new NotFoundException(errorMsg);
                    });
            log.info("Фильм найден: id={}, name={}", film.getId(), film.getName());
            return film;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении фильма по id={}: {}", id, e.getMessage());
            throw new RuntimeException("Не удалось получить фильм", e);
        }
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Начало добавления лайка: filmId={}, userId={}", filmId, userId);
        try {
            getById(filmId); // Проверяем существование фильма
            validateUserExists(userId);
            filmStorage.addLike(filmId, userId);
            eventService.recordLikeEvent(userId, filmId, Event.Operation.ADD);
            log.info("Лайк успешно добавлен: filmId={}, userId={}", filmId, userId);
        } catch (NotFoundException e) {
            log.error("Ошибка при добавлении лайка: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при добавлении лайка filmId={}, userId={}: {}",
                    filmId, userId, e.getMessage());
            throw new RuntimeException("Не удалось добавить лайк", e);
        }
    }

    public void deleteLike(Long filmId, Long userId) {
        log.info("Начало удаления лайка: filmId={}, userId={}", filmId, userId);
        try {
            getById(filmId); // Проверяем существование фильма
            validateUserExists(userId);
            filmStorage.deleteLike(filmId, userId);
            eventService.recordLikeEvent(userId, filmId, Event.Operation.REMOVE);
            log.info("Лайк успешно удалён: filmId={}, userId={}", filmId, userId);
        } catch (NotFoundException e) {
            log.error("Ошибка при удалении лайка: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при удалении лайка filmId={}, userId={}: {}",
                    filmId, userId, e.getMessage());
            throw new RuntimeException("Не удалось удалить лайк", e);
        }
    }

    public List<Film> getPopular(int count) {
        log.info("Запрос популярных фильмов: count={}", count);
        try {
            if (count <= 0) {
                String errorMsg = "Параметр count должен быть больше 0";
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            List<Film> films = filmStorage.getPopular(count);
            log.info("Успешно получено {} популярных фильмов", films.size());
            return films;
        } catch (IllegalArgumentException e) {
            log.error("Ошибка параметра при запросе популярных фильмов: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении популярных фильмов: {}", e.getMessage());
            throw new RuntimeException("Не удалось получить популярные фильмы", e);
        }
    }

    private void validateFilm(Film film) {
        log.debug("Начало валидации фильма: name={}, releaseDate={}", film.getName(), film.getReleaseDate());

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            String errorMsg = "Дата релиза — не раньше 28 декабря 1895 года";
            log.error(errorMsg);
            throw new ValidationException(errorMsg);
        }

        if (film.getMpa() != null && film.getMpa().getId() != null) {
            if (mpaStorage.findById(film.getMpa().getId()).isEmpty()) {
                String errorMsg = String.format("Рейтинг MPA с id=%d не найден", film.getMpa().getId());
                log.error(errorMsg);
                throw new NotFoundException(errorMsg);
            }
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (genre.getId() != null && genreStorage.findById(genre.getId()).isEmpty()) {
                    String errorMsg = String.format("Жанр с id=%d не найден", genre.getId());
                    log.error(errorMsg);
                    throw new NotFoundException(errorMsg);
                }
            }
        }

        log.debug("Валидация фильма прошла успешно");
    }

    private void validateUserExists(Long userId) {
        log.debug("Проверка существования пользователя id={}", userId);
        if (userStorage.getById(userId).isEmpty()) {
            String errorMsg = String.format("Пользователь с id=%d не найден", userId);
            log.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        log.debug("Пользователь id={} существует", userId);
    }
}