package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;
    private final EventService eventService;

    public UserService(UserStorage userStorage, EventService eventService) {
        this.userStorage = userStorage;
        this.eventService = eventService;
    }

    public Collection<User> findAll() {
        log.info("Fetching all users");
        return userStorage.findAll();
    }

    public User create(User user) {
        log.info("Creating user: {}", user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        User created = userStorage.create(user);
        log.info("User created with id: {}", created.getId());
        return created;
    }

    public User update(User user) {
        log.info("Updating user id: {}", user.getId());
        getUserOrThrow(user.getId());
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        User updated = userStorage.update(user);
        log.info("User updated successfully");
        return updated;
    }

    public User getById(Long id) {
        log.info("Fetching user by id: {}", id);
        return getUserOrThrow(id);
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Adding friend {} to user {}", friendId, userId);
        getUserOrThrow(userId);
        getUserOrThrow(friendId);
        userStorage.addFriend(userId, friendId);

        // Логируем событие для пользователя, который добавил друга
        eventService.addEvent(userId, friendId, EventType.FRIEND, Operation.ADD);
        log.info("Событие 'Добавление друга' добавлено в ленту пользователя id={}", userId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        log.info("Removing friend {} from user {}", friendId, userId);
        getUserOrThrow(userId);
        getUserOrThrow(friendId);
        userStorage.deleteFriend(userId, friendId);

        // Логируем событие для пользователя, который удалил друга
        eventService.addEvent(userId, friendId, EventType.FRIEND, Operation.REMOVE);
        log.info("Событие 'Удаление друга' добавлено в ленту пользователя id={}", userId);
    }

    public List<User> getFriends(Long userId) {
        log.info("Fetching friends for user {}", userId);
        getUserOrThrow(userId);
        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Fetching common friends between {} and {}", userId, otherId);
        getUserOrThrow(userId);
        getUserOrThrow(otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }

    private User getUserOrThrow(Long id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }
}