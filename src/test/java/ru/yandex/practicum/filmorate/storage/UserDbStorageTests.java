package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTests {

    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM friendship");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update(
                "INSERT INTO users (email, login, name, birthday) VALUES ('anna@example.com', 'anna', 'Анна', '1985-05-12')");
        jdbcTemplate.update(
                "INSERT INTO users (email, login, name, birthday) VALUES ('pavel@example.com', 'pavel', 'Павел', '1992-03-19')");
        jdbcTemplate.update(
                "INSERT INTO users (email, login, name, birthday) VALUES ('maria@example.com', 'maria', 'Мария', '2001-11-30')");
    }

    @Test
    void shouldCreateAndFindUserById() {
        User user = new User();
        user.setEmail("testcreate@example.com");
        user.setLogin("testcreate");
        user.setName("Созданный пользователь");
        user.setBirthday(LocalDate.of(1995, 6, 15));

        User created = userStorage.create(user);

        Optional<User> found = userStorage.getById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("testcreate@example.com");
        assertThat(found.get().getLogin()).isEqualTo("testcreate");
        assertThat(found.get().getName()).isEqualTo("Созданный пользователь");
        assertThat(found.get().getBirthday()).isEqualTo(LocalDate.of(1995, 6, 15));
    }

    @Test
    void shouldUpdateExistingUser() {
        User user = userStorage.findAll().get(0);

        user.setName("Новое Имя После Обновления");
        user.setEmail("updated@example.com");

        User updated = userStorage.update(user);

        User fromDb = userStorage.getById(updated.getId()).orElseThrow();

        assertThat(fromDb.getName()).isEqualTo("Новое Имя После Обновления");
        assertThat(fromDb.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void shouldFindAllUsers() {
        List<User> allUsers = userStorage.findAll();
        assertThat(allUsers).hasSize(3);
    }

    @Test
    void shouldAddAndRemoveFriend() {
        List<User> users = userStorage.findAll();

        Long userId = users.get(0).getId();
        Long friendId = users.get(1).getId();

        userStorage.addFriend(userId, friendId);

        List<User> friends = userStorage.getFriends(userId);
        assertThat(friends).extracting(User::getId).contains(friendId);

        userStorage.deleteFriend(userId, friendId);

        List<User> friendsAfterDelete = userStorage.getFriends(userId);
        assertThat(friendsAfterDelete).isEmpty();
    }

    @Test
    void shouldNotFindNonExistentUser() {
        Optional<User> user = userStorage.getById(9999L);
        assertThat(user).isEmpty();
    }
}
