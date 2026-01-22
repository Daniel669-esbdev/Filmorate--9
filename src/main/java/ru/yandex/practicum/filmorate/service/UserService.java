package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
        log.info("UserService инициализирован");
    }

    public Collection<User> findAll() {
        log.info("Запрос на получение всех пользователей");
        try {
            Collection<User> users = userStorage.findAll();
            log.info("Успешно получено {} пользователей", users.size());
            return users;
        } catch (Exception e) {
            log.error("Ошибка при получении всех пользователей: {}", e.getMessage());
            throw new RuntimeException("Не удалось получить список пользователей", e);
        }
    }

    public User create(User user) {
        log.info("Начало создания пользователя: login={}, email={}", user.getLogin(), user.getEmail());
        try {
            if (user.getName() == null || user.getName().isBlank()) {
                log.info("Имя пользователя не указано, используется login={}", user.getLogin());
                user.setName(user.getLogin());
            }
            User created = userStorage.create(user);
            log.info("Пользователь успешно создан: id={}, login={}", created.getId(), created.getLogin());
            return created;
        } catch (Exception e) {
            log.error("Ошибка при создании пользователя: {}", e.getMessage());
            throw new RuntimeException("Не удалось создать пользователя", e);
        }
    }

    public User update(User user) {
        log.info("Начало обновления пользователя id={}, login={}", user.getId(), user.getLogin());
        try {
            getUserOrThrow(user.getId());
            if (user.getName() == null || user.getName().isBlank()) {
                log.info("Имя пользователя не указано, используется login={}", user.getLogin());
                user.setName(user.getLogin());
            }
            User updated = userStorage.update(user);
            log.info("Пользователь успешно обновлён: id={}, login={}", updated.getId(), updated.getLogin());
            return updated;
        } catch (NotFoundException e) {
            log.error("Ошибка при обновлении пользователя: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обновлении пользователя id={}: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Не удалось обновить пользователя", e);
        }
    }

    public User getById(Long id) {
        log.info("Запрос пользователя по id={}", id);
        try {
            User user = getUserOrThrow(id);
            log.info("Пользователь найден: id={}, login={}", user.getId(), user.getLogin());
            return user;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении пользователя по id={}: {}", id, e.getMessage());
            throw new RuntimeException("Не удалось получить пользователя", e);
        }
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Начало добавления друга: userId={}, friendId={}", userId, friendId);
        try {
            getUserOrThrow(userId);
            getUserOrThrow(friendId);
            userStorage.addFriend(userId, friendId);
            log.info("Друг успешно добавлен: userId={}, friendId={}", userId, friendId);
        } catch (NotFoundException e) {
            log.error("Ошибка при добавлении друга: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при добавлении друга userId={}, friendId={}: {}",
                    userId, friendId, e.getMessage());
            throw new RuntimeException("Не удалось добавить друга", e);
        }
    }

    public void deleteFriend(Long userId, Long friendId) {
        log.info("Начало удаления друга: userId={}, friendId={}", userId, friendId);
        try {
            getUserOrThrow(userId);
            getUserOrThrow(friendId);
            userStorage.deleteFriend(userId, friendId);
            log.info("Друг успешно удалён: userId={}, friendId={}", userId, friendId);
        } catch (NotFoundException e) {
            log.error("Ошибка при удалении друга: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при удаления друга userId={}, friendId={}: {}",
                    userId, friendId, e.getMessage());
            throw new RuntimeException("Не удалось удалить друга", e);
        }
    }

    public List<User> getFriends(Long userId) {
        log.info("Запрос друзей пользователя id={}", userId);
        try {
            getUserOrThrow(userId);
            List<User> friends = userStorage.getFriends(userId);
            log.info("Успешно получено {} друзей для пользователя id={}", friends.size(), userId);
            return friends;
        } catch (NotFoundException e) {
            log.error("Ошибка при получении друзей: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении друзей пользователя id={}: {}", userId, e.getMessage());
            throw new RuntimeException("Не удалось получить список друзей", e);
        }
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Запрос общих друзей: userId={}, otherId={}", userId, otherId);
        try {
            getUserOrThrow(userId);
            getUserOrThrow(otherId);
            List<User> commonFriends = userStorage.getCommonFriends(userId, otherId);
            log.info("Успешно получено {} общих друзей для пользователей {} и {}",
                    commonFriends.size(), userId, otherId);
            return commonFriends;
        } catch (NotFoundException e) {
            log.error("Ошибка при получении общих друзей: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при получении общих друзей userId={}, otherId={}: {}",
                    userId, otherId, e.getMessage());
            throw new RuntimeException("Не удалось получить общих друзей", e);
        }
    }

    private User getUserOrThrow(Long id) {
        try {
            return userStorage.getById(id)
                    .orElseThrow(() -> {
                        String errorMsg = String.format("Пользователь с id=%d не найден", id);
                        log.error(errorMsg);
                        return new NotFoundException(errorMsg);
                    });
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при поиске пользователя id={}: {}", id, e.getMessage());
            throw new RuntimeException("Не удалось найти пользователя", e);
        }
    }
}