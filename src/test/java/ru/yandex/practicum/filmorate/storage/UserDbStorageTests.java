package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTests {

    private final UserDbStorage userStorage;

    @Test
    @Sql(scripts = {"/sql/clean.sql", "/sql/data-users.sql"})
    void shouldCreateAndFindUserById() {
        User user = new User();
        user.setEmail("testcreate@example.com");
        user.setLogin("testcreate");
        user.setName("Созданный пользователь");
        user.setBirthday(LocalDate.of(1995, 6, 15));

        User created = userStorage.create(user);

        assertThat(created.getId()).isNotNull().isGreaterThan(0L);

        Optional<User> found = userStorage.getById(created.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("testcreate@example.com");
        assertThat(found.get().getLogin()).isEqualTo("testcreate");
        assertThat(found.get().getName()).isEqualTo("Созданный пользователь");
        assertThat(found.get().getBirthday()).isEqualTo(LocalDate.of(1995, 6, 15));
    }

    @Test
    @Sql(scripts = {"/sql/clean.sql", "/sql/data-users.sql"})
    void shouldUpdateExistingUser() {
        Optional<User> userOptional = userStorage.getById(1L);
        assertThat(userOptional).isPresent();

        User user = userOptional.get();
        String newName = "Новое Имя После Обновления";
        String newEmail = "updated@example.com";

        user.setName(newName);
        user.setEmail(newEmail);

        User updated = userStorage.update(user);

        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getEmail()).isEqualTo(newEmail);

        User fromDb = userStorage.getById(1L).orElseThrow();
        assertThat(fromDb.getName()).isEqualTo(newName);
        assertThat(fromDb.getEmail()).isEqualTo(newEmail);
    }

    @Test
    @Sql(scripts = {"/sql/clean.sql", "/sql/data-users.sql"})
    void shouldFindAllUsers() {
        List<User> allUsers = userStorage.findAll();

        assertThat(allUsers).isNotEmpty();
        assertThat(allUsers).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @Sql(scripts = {"/sql/clean.sql", "/sql/data-users.sql"})
    void shouldAddAndRemoveFriend() {
        Long userId = 1L;
        Long friendId = 2L;

        userStorage.addFriend(userId, friendId);

        List<User> friends = userStorage.getFriends(userId);
        assertThat(friends).isNotEmpty();
        assertThat(friends).extracting(User::getId).contains(friendId);

        userStorage.deleteFriend(userId, friendId);

        List<User> friendsAfterDelete = userStorage.getFriends(userId);
        assertThat(friendsAfterDelete)
                .extracting(User::getId)
                .doesNotContain(friendId);
    }

    @Test
    @Sql(scripts = {"/sql/clean.sql", "/sql/data-users.sql"})
    void shouldNotFindNonExistentUser() {
        Optional<User> user = userStorage.getById(9999L);
        assertThat(user).isEmpty();
    }
}