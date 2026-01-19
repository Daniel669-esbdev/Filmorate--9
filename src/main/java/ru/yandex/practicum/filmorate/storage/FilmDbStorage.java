package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
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
        loadAllDataForFilms(films);
        return films;
    }

    @Override
    public Film create(Film film) {
        log.info("Создание фильма: {}", film.getName());
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
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
        saveDirectors(id, film.getDirectors());

        return getById(id).orElseThrow();
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, film.getName(), film.getDescription(),
                java.sql.Date.valueOf(film.getReleaseDate()), film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null, film.getId());

        if (rows == 0) throw new NotFoundException("Фильм не найден");

        saveGenres(film.getId(), film.getGenres());
        saveDirectors(film.getId(), film.getDirectors());

        return getById(film.getId()).orElseThrow();
    }

    @Override
    public Optional<Film> getById(Long id) {
        String sql = "SELECT f.*, m.id AS mpa_id, m.name AS mpa_name FROM films f LEFT JOIN mpa m ON f.mpa_id = m.id WHERE f.id = ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) return Optional.empty();

        loadAllDataForFilms(films);
        return Optional.of(films.get(0));
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbcTemplate.update("MERGE INTO film_likes (film_id, user_id) VALUES (?, ?)", filmId, userId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?", filmId, userId);
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = """
            SELECT f.*, m.id AS mpa_id, m.name AS mpa_name, COUNT(fl.user_id) AS likes_count
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.id
            LEFT JOIN film_likes fl ON f.id = fl.film_id
            GROUP BY f.id, m.id, m.name
            ORDER BY likes_count DESC, f.id ASC LIMIT ?
            """;
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);
        loadAllDataForFilms(films);
        return films;
    }

    @Override
    public List<Film> search(String query, String by) {
        String pattern = "%" + query.toLowerCase() + "%";
        StringBuilder sql = new StringBuilder(
                "SELECT f.*, m.id AS mpa_id, m.name AS mpa_name, COUNT(DISTINCT fl.user_id) AS likes_count " +
                        "FROM films f " +
                        "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                        "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                        "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                        "LEFT JOIN directors d ON fd.director_id = d.id WHERE "
        );

        List<Object> params = new ArrayList<>();
        if (by.contains("director") && by.contains("title")) {
            sql.append("(LOWER(f.name) LIKE ? OR LOWER(d.name) LIKE ?) ");
            params.add(pattern); params.add(pattern);
        } else if (by.contains("director")) {
            sql.append("LOWER(d.name) LIKE ? ");
            params.add(pattern);
        } else {
            sql.append("LOWER(f.name) LIKE ? ");
            params.add(pattern);
        }

        sql.append("GROUP BY f.id, m.id, m.name ORDER BY likes_count DESC");
        List<Film> films = jdbcTemplate.query(sql.toString(), this::mapRowToFilm, params.toArray());
        loadAllDataForFilms(films);
        return films;
    }

    private void loadAllDataForFilms(List<Film> films) {
        if (films.isEmpty()) return;
        loadGenresAndLikesForFilms(films);
        loadDirectorsForFilms(films);
    }

    private void loadDirectorsForFilms(List<Film> films) {
        List<Long> ids = films.stream().map(Film::getId).toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT fd.film_id, d.id, d.name FROM film_directors fd JOIN directors d ON fd.director_id = d.id WHERE fd.film_id IN (" + placeholders + ")";

        Map<Long, Set<Director>> directorMap = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            long filmId = rs.getLong("film_id");
            Director d = new Director(rs.getInt("id"), rs.getString("name"));
            directorMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(d);
        }, ids.toArray());

        films.forEach(f -> f.setDirectors(directorMap.getOrDefault(f.getId(), new LinkedHashSet<>())));
    }

    private void saveDirectors(long filmId, Set<Director> directors) {
        jdbcTemplate.update("DELETE FROM film_directors WHERE film_id = ?", filmId);
        if (directors == null || directors.isEmpty()) return;
        String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
        directors.forEach(d -> jdbcTemplate.update(sql, filmId, d.getId()));
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        if (rs.getObject("mpa_id") != null) {
            film.setMpa(new Mpa(rs.getInt("mpa_id"), rs.getString("mpa_name")));
        }
        film.setGenres(new LinkedHashSet<>());
        film.setLikes(new HashSet<>());
        film.setDirectors(new LinkedHashSet<>());
        return film;
    }

    private void saveGenres(long filmId, Set<Genre> genres) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);
        if (genres == null || genres.isEmpty()) return;
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        genres.forEach(g -> jdbcTemplate.update(sql, filmId, g.getId()));
    }

    private void loadGenresAndLikesForFilms(List<Film> films) {
        List<Long> ids = films.stream().map(Film::getId).toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));

        String genreSql = "SELECT fg.film_id, g.id, g.name FROM film_genres fg JOIN genres g ON fg.genre_id = g.id WHERE fg.film_id IN (" + placeholders + ")";
        Map<Long, Set<Genre>> genresMap = new HashMap<>();
        jdbcTemplate.query(genreSql, rs -> {
            Genre g = new Genre(); g.setId(rs.getInt("id")); g.setName(rs.getString("name"));
            genresMap.computeIfAbsent(rs.getLong("film_id"), k -> new LinkedHashSet<>()).add(g);
        }, ids.toArray());

        String likesSql = "SELECT film_id, user_id FROM film_likes WHERE film_id IN (" + placeholders + ")";
        Map<Long, Set<Long>> likesMap = new HashMap<>();
        jdbcTemplate.query(likesSql, rs -> {
            likesMap.computeIfAbsent(rs.getLong("film_id"), k -> new HashSet<>()).add(rs.getLong("user_id"));
        }, ids.toArray());

        films.forEach(f -> {
            f.setGenres(genresMap.getOrDefault(f.getId(), new LinkedHashSet<>()));
            f.setLikes(likesMap.getOrDefault(f.getId(), new HashSet<>()));
        });
    }
}