package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        log.info("Fetching all users");
        return userStorage.findAll();
    }

    public User create(User user) {
        log.info("Creating user: {}", user);
        User created = userStorage.create(user);
        log.info("User created with id: {}", created.getId());
        return created;
    }

    public User update(User user) {
        log.info("Updating user id: {}", user.getId());
        User existing = userStorage.getById(user.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + user.getId() + " не найден"));
        User updated = userStorage.update(user);
        log.info("User updated successfully");
        return updated;
    }

    public User getById(Long id) {
        log.info("Fetching user by id: {}", id);
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Adding friend {} to user {}", friendId, userId);
        User user = getById(userId);
        User friend = getById(friendId);

        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        if (friend.getFriends() == null) {
            friend.setFriends(new HashSet<>());
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        log.info("Friend added successfully");
    }

    public void deleteFriend(Long userId, Long friendId) {
        log.info("Removing friend {} from user {}", friendId, userId);
        User user = getById(userId);
        User friend = getById(friendId);

        if (user.getFriends() != null) {
            user.getFriends().remove(friendId);
        }
        if (friend.getFriends() != null) {
            friend.getFriends().remove(userId);
        }

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Friend removed successfully");
    }

    public List<User> getFriends(Long userId) {
        log.info("Fetching friends for user {}", userId);
        User user = getById(userId);
        if (user.getFriends() == null) {
            return List.of();
        }
        return user.getFriends().stream()
                .map(userStorage::getById)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Friend not found")))
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Fetching common friends between {} and {}", userId, otherId);
        User user = getById(userId);
        User other = getById(otherId);

        if (user.getFriends() == null || other.getFriends() == null) {
            return List.of();
        }

        return user.getFriends().stream()
                .filter(other.getFriends()::contains)
                .map(userStorage::getById)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Common friend not found")))
                .collect(Collectors.toList());
    }
}