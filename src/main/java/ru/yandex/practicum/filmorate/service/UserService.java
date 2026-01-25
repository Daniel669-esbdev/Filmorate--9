package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FeedStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;
    private final FeedStorage feedStorage;
    private final RecommendationService recommendationService;

    public UserService(UserStorage userStorage,
                       FeedStorage feedStorage,
                       RecommendationService recommendationService) {
        this.userStorage = userStorage;
        this.feedStorage = feedStorage;
        this.recommendationService = recommendationService;
        log.info("UserService успешно инициализирован");
    }

    public Collection<User> findAll() {
        log.info("Запрос на получение всех пользователей");
        return userStorage.findAll();
    }

    public User create(User user) {
        log.info("Создание пользователя: login={}", user.getLogin());
        validateUser(user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.create(user);
    }

    public User update(User user) {
        log.info("Обновление пользователя: id={}", user.getId());
        getUserOrThrow(user.getId());
        validateUser(user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.update(user);
    }

    public User getById(Long id) {
        log.info("Получение пользователя по id={}", id);
        return getUserOrThrow(id);
    }

    public void deleteUser(Long id) {
        log.info("Удаление пользователя с id = {}", id);
        getUserOrThrow(id);
        userStorage.deleteUser(id);
        log.info("Удаление пользователя id = {} успешно выполнено", id);
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Пользователь {} добавляет в друзья {}", userId, friendId);
        getUserOrThrow(userId);
        getUserOrThrow(friendId);
        userStorage.addFriend(userId, friendId);
        feedStorage.addEvent(userId, "FRIEND", "ADD", friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        log.info("Пользователь {} удаляет из друзей {}", userId, friendId);
        getUserOrThrow(userId);
        getUserOrThrow(friendId);
        userStorage.deleteFriend(userId, friendId);
        feedStorage.addEvent(userId, "FRIEND", "REMOVE", friendId);
    }

    public List<User> getFriends(Long userId) {
        log.info("Запрос списка друзей пользователя {}", userId);
        getUserOrThrow(userId);
        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Запрос общих друзей пользователей {} и {}", userId, otherId);
        getUserOrThrow(userId);
        getUserOrThrow(otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }

    public List<Event> getFeed(Long userId) {
        log.info("Запрос ленты событий пользователя {}", userId);
        getUserOrThrow(userId);
        return feedStorage.getFeed(userId);
    }

    public List<Film> getRecommendations(Long userId) {
        log.info("Запрос рекомендаций для пользователя {}", userId);
        getUserOrThrow(userId);
        return recommendationService.getRecommendations(userId);
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getBirthday() == null) {
            throw new ValidationException("Дата рождения должна быть указана");
        }
    }

    private User getUserOrThrow(Long id) {
        return userStorage.getById(id)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", id);
                    return new NotFoundException(String.format("Пользователь с id=%d не найден", id));
                });
    }
}