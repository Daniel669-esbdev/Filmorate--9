 /* package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserService userService;
    private InMemoryUserStorage userStorage;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
    }

    @Test
    void addAndDeleteFriend() {
        User user1 = createUser("user1@example.com", "user1");
        User user2 = createUser("user2@example.com", "user2");

        userService.addFriend(user1.getId(), user2.getId());

        List<User> friends1 = userService.getFriends(user1.getId());
        List<User> friends2 = userService.getFriends(user2.getId());

        assertEquals(1, friends1.size());
        assertEquals(user2.getId(), friends1.get(0).getId());
        assertEquals(1, friends2.size());
        assertEquals(user1.getId(), friends2.get(0).getId());

        userService.deleteFriend(user1.getId(), user2.getId());

        assertTrue(userService.getFriends(user1.getId()).isEmpty());
        assertTrue(userService.getFriends(user2.getId()).isEmpty());
    }

    @Test
    void getCommonFriends() {
        User user1 = createUser("u1@example.com", "u1");
        User user2 = createUser("u2@example.com", "u2");
        User common = createUser("common@example.com", "common");

        userService.addFriend(user1.getId(), common.getId());
        userService.addFriend(user2.getId(), common.getId());

        List<User> commonFriends = userService.getCommonFriends(user1.getId(), user2.getId());

        assertEquals(1, commonFriends.size());
        assertEquals(common.getId(), commonFriends.get(0).getId());
    }

    private User createUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userService.create(user);
    }
} */