package ru.yandex.practicum.filmorate.storage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

@Component
@AllArgsConstructor
@Slf4j
public class DirectorDBStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Director> directorRowMapper = (rs, rowNum) -> {
        Director director = new Director();

        director.setId(rs.getInt("id"));
        director.setName(rs.getString("name"));

        return director;
    };

    @Override
    public List<Director> getAllDirectors() {
        String selectAllDirectorsSql = """
                    SELECT *
                    FROM directors
                """;
        return jdbcTemplate.query(selectAllDirectorsSql, directorRowMapper);
    }

    @Override
    public Optional<Director> getDirectorById(int directorId) {
        String selectDirectorByIdSql = """
                    SELECT *
                    FROM directors
                    WHERE id = ?
                """;

        return jdbcTemplate.query(selectDirectorByIdSql, directorRowMapper, directorId)
                .stream()
                .findFirst();
    }

    @Override
    public Director createDirector(Director director) {
        String insertDirectorQuery = """
                    INSERT INTO directors (name)
                    VALUES (?)
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(insertDirectorQuery, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, director.getName());
            return ps;
        }, keyHolder);

        Integer directorId = keyHolder.getKeyAs(Integer.class);

        director.setId(directorId);

        return director;
    }

    @Override
    public Director updateDirector(Director director) {
        String updateDirectorQuery = """
                    UPDATE directors
                    SET name = ?
                    WHERE id = ?
                """;
        int updatedRows = jdbcTemplate.update(
                updateDirectorQuery,
                director.getName(),
                director.getId());

        if (updatedRows != 1) {
            throw new NotFoundException("Режиссер не найден");
        }

        return director;
    }

    @Override
    public void deleteDirectorById(int directorId) {
        String deleteDirectorByIdQuery = """
                    DELETE FROM directors
                    WHERE id = ?
                """;

        int updatedRows = jdbcTemplate.update(deleteDirectorByIdQuery, directorId);
        if (updatedRows != 1) {
            throw new NotFoundException("Режиссер не найден");
        }
    }

}