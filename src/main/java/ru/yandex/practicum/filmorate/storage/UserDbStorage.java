package ru.yandex.practicum.filmorate.storage;

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

@Component
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);
        loadFriendsForUsers(users);
        return users;
    }

    @Override
    public User create(User user) {
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
        return user;
    }

    @Override
    public User update(User user) {
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
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser, id);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        User user = users.get(0);
        loadFriendsForUser(user);

        return Optional.of(user);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sql = "INSERT INTO friendship (user_id, friend_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        jdbcTemplate.update(sql, userId, friendId);
        jdbcTemplate.update(sql, friendId, userId);
    }

    @Override
    public void deleteFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendship WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        jdbcTemplate.update(sql, userId, friendId, friendId, userId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = """
                SELECT u.* FROM users u
                JOIN friendship f ON u.id = f.friend_id
                WHERE f.user_id = ?
                """;
        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        String sql = """
                SELECT u.* FROM users u
                JOIN friendship f1 ON u.id = f1.friend_id AND f1.user_id = ?
                JOIN friendship f2 ON u.id = f2.friend_id AND f2.user_id = ?
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
        user.setFriends(new HashSet<>());
        return user;
    }

    private void loadFriendsForUser(User user) {
        String sql = "SELECT friend_id FROM friendship WHERE user_id = ?";
        Set<Long> friends = new HashSet<>(
                jdbcTemplate.queryForList(sql, Long.class, user.getId())
        );
        user.setFriends(friends);
    }

    private void loadFriendsForUsers(List<User> users) {
        if (users.isEmpty()) return;

        String placeholders = String.join(",", Collections.nCopies(users.size(), "?"));
        String sql = "SELECT user_id, friend_id FROM friendship WHERE user_id IN (" + placeholders + ")";

        List<Long> ids = users.stream().map(User::getId).toList();

        Map<Long, Set<Long>> friendsMap = new HashMap<>();
        jdbcTemplate.query(sql,
                rs -> {
                    long userId = rs.getLong("user_id");
                    long friendId = rs.getLong("friend_id");
                    friendsMap.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
                },
                ids.toArray(new Long[0])
        );

        for (User user : users) {
            user.setFriends(friendsMap.getOrDefault(user.getId(), new HashSet<>()));
        }
    }
}