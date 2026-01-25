package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<User> findAll() {
        log.debug("Запрос на получение всех пользователей");
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    @Override
    public User create(User user) {
        log.info("Создание пользователя: email={}, login={}", user.getEmail(), user.getLogin());
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, java.sql.Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);

        user.setId(keyHolder.getKey().longValue());
        log.info("Пользователь создан, id={}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        log.info("Обновление пользователя id={}", user.getId());
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        int rows = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                java.sql.Date.valueOf(user.getBirthday()),
                user.getId());

        if (rows == 0) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }

        return getById(user.getId()).orElseThrow();
    }

    @Override
    public Optional<User> getById(Long id) {
        log.debug("Запрос пользователя по id={}", id);
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser, id);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(users.get(0));
    }

    @Override
    public void deleteUser(Long id) {
        log.debug("Запрос на удаление пользователя из БД, id = {}", id);

        String sql = "DELETE FROM users WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);

        if (rows == 0) {
            throw new NotFoundException("Пользователь с id '" + id + "' не найден");
        }
        log.debug("Строк из users удалено: {}", rows);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление в друзья: userId={}, friendId={}", userId, friendId);
        String checkSql = "SELECT COUNT(*) FROM friendship WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count == 0) {
            String insertSql = "INSERT INTO friendship (user_id, friend_id) VALUES (?, ?)";
            jdbcTemplate.update(insertSql, userId, friendId);
        }
    }

    @Override
    public void deleteFriend(Long userId, Long friendId) {
        log.info("Удаление из друзей: userId={}, friendId={}", userId, friendId);
        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        log.debug("Получение списка друзей пользователя id={}", userId);
        String sql = """
                SELECT u.* FROM users u
                JOIN friendship f ON u.id = f.friend_id
                WHERE f.user_id = ?
                ORDER BY u.id
                """;
        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.debug("Получение общих друзей пользователей id={} и id={}", userId, otherId);
        String sql = """
                SELECT u.* FROM users u
                JOIN friendship f1 ON u.id = f1.friend_id AND f1.user_id = ?
                JOIN friendship f2 ON u.id = f2.friend_id AND f2.user_id = ?
                ORDER BY u.id
                """;
        return jdbcTemplate.query(sql, this::mapRowToUser, userId, otherId);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    }

    @Override
    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}