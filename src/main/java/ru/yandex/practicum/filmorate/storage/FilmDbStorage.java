package ru.yandex.practicum.filmorate.storage;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@Primary
@AllArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final RowMapper<Director> directorRowMapper = (rs, rowNum) -> {
        Director director = new Director();
        director.setId(rs.getInt("id"));
        director.setName(rs.getString("name"));
        return director;
    };

    private final ResultSetExtractor<Map<Long, List<Director>>> directorByFilmMapper = rs -> {
        Map<Long, List<Director>> result = new HashMap<>();
        while (rs.next()) {
            Long filmId = rs.getLong("film_id");
            Director director = new Director();
            director.setId(rs.getInt("id"));
            director.setName(rs.getString("name"));
            result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(director);
        }
        return result;
    };

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa m ON f.mpa_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);
        loadDataForFilms(films);
        return films;
    }

    @Override
    public Film create(Film film) {
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
        updateDirectorsByFilmId(id, film.getDirectors());
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
        updateDirectorsByFilmId(film.getId(), film.getDirectors());
        return getById(film.getId()).orElseThrow();
    }

    @Override
    public Optional<Film> getById(Long id) {
        String sql = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa m ON f.mpa_id = m.id WHERE f.id = ?";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        if (films.isEmpty()) return Optional.empty();
        loadDataForFilms(films);
        return Optional.of(films.get(0));
    }

    @Override
    public void deleteFilm(Long id) {
        int rows = jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
        if (rows == 0) {
            throw new NotFoundException("Фильм с id '" + id + "' не найден");
        }
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbcTemplate.update("INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)", filmId, userId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        int rows = jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?", filmId, userId);
        if (rows == 0) throw new NotFoundException("Лайк не найден");
    }

    @Override
    public List<Film> search(String query, String by) {
        String pattern = "%" + query.toLowerCase() + "%";
        StringBuilder sql = new StringBuilder(
                "SELECT f.*, m.name AS mpa_name, COUNT(DISTINCT fl.user_id) AS likes_count " +
                        "FROM films f " +
                        "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                        "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                        "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                        "LEFT JOIN directors d ON fd.director_id = d.id WHERE "
        );

        List<Object> params = new ArrayList<>();
        if (by.contains("director") && by.contains("title")) {
            sql.append("(LOWER(f.name) LIKE ? OR LOWER(d.name) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
        } else if (by.contains("director")) {
            sql.append("LOWER(d.name) LIKE ? ");
            params.add(pattern);
        } else {
            sql.append("LOWER(f.name) LIKE ? ");
            params.add(pattern);
        }

        sql.append("GROUP BY f.id, m.name ORDER BY likes_count DESC");
        List<Film> films = jdbcTemplate.query(sql.toString(), this::mapRowToFilm, params.toArray());

        loadDataForFilms(films);
        return films;
    }

    @Override
    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT f.*, m.name AS mpa_name, COUNT(fl.user_id) AS likes_count " +
                        "FROM films f " +
                        "LEFT JOIN mpa m ON f.mpa_id = m.id " +
                        "LEFT JOIN film_likes fl ON f.id = fl.film_id "
        );

        if (genreId != null) {
            sql.append("JOIN film_genres fg ON f.id = fg.film_id ");
        }
        sql.append("WHERE 1=1 ");
        if (genreId != null) {
            sql.append("AND fg.genre_id = ? ");
            params.add(genreId);
        }
        if (year != null) {
            sql.append("AND EXTRACT(YEAR FROM f.release_date) = ? ");
            params.add(year);
        }
        sql.append("GROUP BY f.id, m.name ORDER BY likes_count DESC, f.id ASC LIMIT ?");
        params.add(count);

        List<Film> films = jdbcTemplate.query(sql.toString(), this::mapRowToFilm, params.toArray());
        loadDataForFilms(films);
        return films;
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = """
                SELECT f.*, m.name AS mpa_name, COUNT(fl3.user_id) AS likes_count
                FROM films f
                JOIN film_likes fl1 ON f.id = fl1.film_id AND fl1.user_id = ?
                JOIN film_likes fl2 ON f.id = fl2.film_id AND fl2.user_id = ?
                LEFT JOIN film_likes fl3 ON f.id = fl3.film_id
                LEFT JOIN mpa m ON f.mpa_id = m.id
                GROUP BY f.id, m.name
                ORDER BY likes_count DESC, f.id ASC
                """;
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, userId, friendId);
        loadDataForFilms(films);
        return films;
    }

    @Override
    public List<Film> findAllBy(Integer directorId, String sortBy) {
        String sql = "SELECT f.*, m.name AS mpa_name, COUNT(fl.user_id) AS likes_count " +
                "FROM films f LEFT JOIN mpa m ON f.mpa_id = m.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "JOIN film_directors fd ON f.id = fd.film_id WHERE fd.director_id = ? " +
                "GROUP BY f.id, m.name ";

        if ("likes".equals(sortBy)) {
            sql += "ORDER BY likes_count DESC";
        } else {
            sql += "ORDER BY f.release_date ASC";
        }

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, directorId);
        loadDataForFilms(films);
        return films;
    }

    private void loadDataForFilms(List<Film> films) {
        if (films.isEmpty()) return;
        List<Long> ids = films.stream().map(Film::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);

        String genreSql = "SELECT fg.film_id, g.id, g.name FROM film_genres fg JOIN genres g ON fg.genre_id = g.id WHERE fg.film_id IN (:ids)";
        Map<Long, Set<Genre>> genresMap = new HashMap<>();
        namedParameterJdbcTemplate.query(genreSql, params, rs -> {
            Genre g = new Genre();
            g.setId(rs.getInt("id"));
            g.setName(rs.getString("name"));
            genresMap.computeIfAbsent(rs.getLong("film_id"), k -> new LinkedHashSet<>()).add(g);
        });

        String likesSql = "SELECT film_id, user_id FROM film_likes WHERE film_id IN (:ids)";
        Map<Long, Set<Long>> likesMap = new HashMap<>();
        namedParameterJdbcTemplate.query(likesSql, params, rs -> {
            likesMap.computeIfAbsent(rs.getLong("film_id"), k -> new HashSet<>()).add(rs.getLong("user_id"));
        });

        String dirSql = "SELECT fd.film_id, d.id, d.name FROM film_directors fd JOIN directors d ON fd.director_id = d.id WHERE fd.film_id IN (:ids)";
        Map<Long, List<Director>> directorsMap = namedParameterJdbcTemplate.query(dirSql, params, directorByFilmMapper);

        films.forEach(f -> {
            f.setGenres(genresMap.getOrDefault(f.getId(), new LinkedHashSet<>()));
            f.setLikes(likesMap.getOrDefault(f.getId(), new HashSet<>()));
            f.setDirectors(directorsMap.getOrDefault(f.getId(), new ArrayList<>()));
        });
    }

    private void updateDirectorsByFilmId(long filmId, List<Director> directors) {
        jdbcTemplate.update("DELETE FROM film_directors WHERE film_id = ?", filmId);
        if (directors == null || directors.isEmpty()) return;

        String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(sql, directors, directors.size(), (ps, d) -> {
            ps.setLong(1, filmId);
            ps.setInt(2, d.getId());
        });
    }

    private void saveGenres(long filmId, Set<Genre> genres) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", filmId);
        if (genres == null || genres.isEmpty()) return;
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        genres.forEach(g -> jdbcTemplate.update(sql, filmId, g.getId()));
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
        return film;
    }
}