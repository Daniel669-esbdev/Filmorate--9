package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Collection<Film> findAll() {
        log.debug("Запрос на получение всех фильмов");
        String sql = """
                SELECT f.*, m.id AS mpa_id, m.name AS mpa_name
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                """;
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        loadGenresAndLikesForFilms(films);
        log.debug("Получено фильмов: {}", films.size());
        return films;
    }

    @Override
    public Film create(Film film) {
        log.info("Создание фильма: name={}, releaseDate={}", film.getName(), film.getReleaseDate());
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, java.sql.Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            stmt.setObject(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return stmt;
        }, keyHolder);

        long id = keyHolder.getKey().longValue();
        film.setId(id);

        saveGenres(id, film.getGenres());
        log.info("Фильм создан, id={}", id);
        return getById(id).orElseThrow();
    }

    @Override
    public Film update(Film film) {
        log.info("Обновление фильма id={}", film.getId());
        String sql = """
                UPDATE films
                SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                java.sql.Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (rows == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        saveGenres(film.getId(), film.getGenres());

        log.info("Фильм id={} успешно обновлён", film.getId());
        return getById(film.getId()).orElseThrow();
    }

    @Override
    public Optional<Film> getById(Long id) {
        log.debug("Запрос фильма по id={}", id);
        String sql = """
                SELECT f.*, m.id AS mpa_id, m.name AS mpa_name
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                WHERE f.id = ?
                """;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) {
            log.debug("Фильм с id={} не найден", id);
            return Optional.empty();
        }

        Film film = films.get(0);
        loadGenresForFilm(film);
        loadLikesForFilm(film);

        log.debug("Фильм найден: id={}, name={}", film.getId(), film.getName());
        return Optional.of(film);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        log.info("Добавление лайка: filmId={}, userId={}", filmId, userId);

        String checkSql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, filmId, userId);

        log.debug("Проверка лайка: уже существует? count={}", count);

        if (count == 0) {
            String insertSql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
            jdbcTemplate.update(insertSql, filmId, userId);
            log.info("Лайк добавлен: filmId={}, userId={}", filmId, userId);
        } else {
            log.info("Лайк уже существует, пропускаем: filmId={}, userId={}", filmId, userId);
        }
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        log.info("Удаление лайка: filmId={}, userId={}", filmId, userId);
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        int rows = jdbcTemplate.update(sql, filmId, userId);
        log.debug("Удалено лайков: {}", rows);
    }

    @Override
    public List<Film> getPopular(int count) {
        log.info("Запрос популярных фильмов: count={}", count);
        String sql = """
            SELECT f.*, m.id AS mpa_id, m.name AS mpa_name,
                   COALESCE(COUNT(DISTINCT fl.user_id), 0) AS likes_count
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.id
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            GROUP BY f.id, m.id, m.name
            ORDER BY likes_count DESC, f.id ASC
            LIMIT ?
            """;

        List<Film> popularFilms = jdbcTemplate.query(sql, this::mapRowToFilm, count);
        log.debug("Найдено популярных фильмов: {}", popularFilms.size());

        loadGenresAndLikesForFilms(popularFilms);
        return popularFilms;
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        if (rs.getObject("mpa_id") != null) {
            Mpa mpa = new Mpa();
            mpa.setId(rs.getInt("mpa_id"));
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);
        }

        film.setGenres(new LinkedHashSet<>());
        film.setLikes(new HashSet<>());
        return film;
    }

    private void loadGenresForFilm(Film film) {
        String sql = """
                SELECT g.id, g.name
                FROM film_genres fg
                JOIN genres g ON fg.genre_id = g.id
                WHERE fg.film_id = ?
                ORDER BY g.id
                """;

        List<Genre> genres = jdbcTemplate.query(sql, (rs, row) -> {
            Genre g = new Genre();
            g.setId(rs.getInt("id"));
            g.setName(rs.getString("name"));
            return g;
        }, film.getId());

        film.setGenres(new LinkedHashSet<>(genres));
    }

    private void loadLikesForFilm(Film film) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        Set<Long> likes = new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, film.getId()));
        film.setLikes(likes);
    }

    private void loadGenresAndLikesForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Long> ids = films.stream().map(Film::getId).toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));

        String genreSql = """
                SELECT fg.film_id, g.id, g.name
                FROM film_genres fg
                JOIN genres g ON fg.genre_id = g.id
                WHERE fg.film_id IN (%s)
                ORDER BY fg.film_id, g.id
                """.formatted(placeholders);

        Map<Long, Set<Genre>> genresMap = new HashMap<>();
        jdbcTemplate.query(genreSql, rs -> {
            long filmId = rs.getLong("film_id");
            Genre g = new Genre();
            g.setId(rs.getInt("id"));
            g.setName(rs.getString("name"));
            genresMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(g);
        }, ids.toArray());

        String likesSql = """
                SELECT film_id, user_id
                FROM film_likes
                WHERE film_id IN (%s)
                """.formatted(placeholders);

        Map<Long, Set<Long>> likesMap = new HashMap<>();
        jdbcTemplate.query(likesSql, rs -> {
            long filmId = rs.getLong("film_id");
            long userId = rs.getLong("user_id");
            likesMap.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        }, ids.toArray());

        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));
            film.setLikes(likesMap.getOrDefault(film.getId(), new HashSet<>()));
        }
    }

    private void saveGenres(long filmId, Set<Genre> genres) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);

        if (genres == null || genres.isEmpty()) return;

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        for (Genre genre : genres) {
            jdbcTemplate.update(sql, filmId, genre.getId());
        }
    }

    private void saveLikes(long filmId, Set<Long> likes) {
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ?", filmId);

        if (likes == null || likes.isEmpty()) return;

        String sql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
        for (Long userId : likes) {
            jdbcTemplate.update(sql, filmId, userId);
        }
    }
}