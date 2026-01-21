package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.RecommendationResponse;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public RecommendationService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        log.info("RecommendationService инициализирован");
    }

    public RecommendationResponse getRecommendations(Long userId) {
        log.info("Начало получения рекомендаций для пользователя id={}", userId);

        validateUserId(userId);

        try {
            Map<Long, List<Long>> allLikes = filmStorage.getAllLikes();
            Set<Long> userLikedFilms = getUserLikedFilms(userId, allLikes);

            if (userLikedFilms.isEmpty()) {
                log.info("У пользователя id={} нет лайков, возвращаем популярные фильмы", userId);
                List<Film> popularFilms = filmStorage.getPopular(10);
                return new RecommendationResponse(userId, popularFilms);
            }

            Long mostSimilarUserId = findMostSimilarUser(userId, allLikes, userLikedFilms);

            if (mostSimilarUserId == null) {
                log.info("Для пользователя id={} не найдено похожих пользователей", userId);
                List<Film> popularFilms = filmStorage.getPopular(10);
                return new RecommendationResponse(userId, popularFilms);
            }

            log.info("Наиболее похожий пользователь для id={}: {}", userId, mostSimilarUserId);

            Set<Long> similarUserLikedFilms = getUserLikedFilms(mostSimilarUserId, allLikes);
            Set<Long> filmsToRecommend = new HashSet<>(similarUserLikedFilms);
            filmsToRecommend.removeAll(userLikedFilms);

            if (filmsToRecommend.isEmpty()) {
                log.info("Нет новых фильмов для рекомендации пользователю id={}", userId);
                List<Film> popularFilms = filmStorage.getPopular(10);
                return new RecommendationResponse(userId, popularFilms);
            }

            List<Film> recommendations = filmsToRecommend.stream()
                    .map(filmId -> filmStorage.getById(filmId).orElse(null))
                    .filter(Objects::nonNull)
                    .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                    .limit(10)
                    .collect(Collectors.toList());

            log.info("Успешно сформировано {} рекомендаций для пользователя id={}",
                    recommendations.size(), userId);
            return new RecommendationResponse(userId, recommendations);

        } catch (NotFoundException e) {
            log.error("Пользователь не найден: id={}, error={}", userId, e.getMessage());
            throw e;

        } catch (IllegalArgumentException e) {
            log.error("Некорректный аргумент: id={}, error={}", userId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Неожиданная ошибка при получении рекомендаций для пользователя id={}",
                    userId, e);
            throw new RuntimeException("Внутренняя ошибка сервера при получении рекомендаций", e);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null");
        }

        if (userId <= 0) {
            throw new IllegalArgumentException("ID пользователя должен быть положительным числом");
        }

        boolean userExists = userStorage.getById(userId).isPresent();
        if (!userExists) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private Long findMostSimilarUser(Long userId, Map<Long, List<Long>> allLikes, Set<Long> userLikedFilms) {
        log.debug("Поиск наиболее похожего пользователя для userId={}", userId);

        Map<Long, Set<Long>> userToFilms = new HashMap<>();

        for (Map.Entry<Long, List<Long>> entry : allLikes.entrySet()) {
            Long filmId = entry.getKey();
            for (Long likedUserId : entry.getValue()) {
                if (!likedUserId.equals(userId)) {
                    userToFilms.computeIfAbsent(likedUserId, k -> new HashSet<>()).add(filmId);
                }
            }
        }

        Long mostSimilarUserId = null;
        int maxIntersection = 0;

        for (Map.Entry<Long, Set<Long>> entry : userToFilms.entrySet()) {
            Long otherUserId = entry.getKey();
            Set<Long> otherUserLikedFilms = entry.getValue();

            Set<Long> intersection = new HashSet<>(userLikedFilms);
            intersection.retainAll(otherUserLikedFilms);

            if (intersection.size() > maxIntersection) {
                maxIntersection = intersection.size();
                mostSimilarUserId = otherUserId;
            }
        }

        log.debug("Найден наиболее похожий пользователь: {} (пересечение: {})",
                mostSimilarUserId, maxIntersection);
        return mostSimilarUserId;
    }

    private Set<Long> getUserLikedFilms(Long userId, Map<Long, List<Long>> allLikes) {
        log.debug("Получение лайкнутых фильмов пользователя id={}", userId);

        return allLikes.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public RecommendationResponse getRecommendationsByPreferences(Long userId) {
        log.info("Начало получения рекомендаций по предпочтениям для пользователя id={}", userId);

        validateUserId(userId);

        try {
            Map<Long, List<Long>> allLikes = filmStorage.getAllLikes();
            Set<Long> userLikedFilms = getUserLikedFilms(userId, allLikes);

            if (userLikedFilms.isEmpty()) {
                log.info("У пользователя id={} нет лайков, используем обычные рекомендации", userId);
                return getRecommendations(userId);
            }

            List<Film> likedFilms = userLikedFilms.stream()
                    .map(filmId -> filmStorage.getById(filmId).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Set<Integer> favoriteGenres = getFavoriteGenres(likedFilms);
            Set<Integer> favoriteMpa = getFavoriteMpa(likedFilms);

            List<Film> notLikedFilms = filmStorage.getFilmsNotLikedByUser(userId);

            List<Film> recommendations = notLikedFilms.stream()
                    .sorted((f1, f2) -> {
                        int score1 = calculateFilmScore(f1, favoriteGenres, favoriteMpa);
                        int score2 = calculateFilmScore(f2, favoriteGenres, favoriteMpa);
                        return Integer.compare(score2, score1);
                    })
                    .limit(10)
                    .collect(Collectors.toList());

            log.info("Успешно сформировано {} рекомендаций по предпочтениям для пользователя id={}",
                    recommendations.size(), userId);
            return new RecommendationResponse(userId, recommendations);

        } catch (NotFoundException e) {
            log.error("Пользователь не найден (preferences): id={}, error={}", userId, e.getMessage());
            throw e;

        } catch (IllegalArgumentException e) {
            log.error("Некорректный аргумент (preferences): id={}, error={}", userId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Неожиданная ошибка при получении рекомендаций по предпочтениям для пользователя id={}",
                    userId, e);
            throw new RuntimeException("Внутренняя ошибка сервера при получении рекомендаций по предпочтениям", e);
        }
    }

    private Set<Integer> getFavoriteGenres(List<Film> likedFilms) {
        log.debug("Получение любимых жанров из {} фильмов", likedFilms.size());

        return likedFilms.stream()
                .flatMap(film -> film.getGenres().stream())
                .map(Genre::getId)
                .collect(Collectors.toSet());
    }

    private Set<Integer> getFavoriteMpa(List<Film> likedFilms) {
        log.debug("Получение любимых рейтингов MPA из {} фильмов", likedFilms.size());

        return likedFilms.stream()
                .map(film -> film.getMpa())
                .filter(Objects::nonNull)
                .map(Mpa::getId)
                .collect(Collectors.toSet());
    }

    private int calculateFilmScore(Film film, Set<Integer> favoriteGenres, Set<Integer> favoriteMpa) {
        int score = 0;

        int genreMatches = (int) film.getGenres().stream()
                .map(Genre::getId)
                .filter(favoriteGenres::contains)
                .count();
        score += genreMatches * 3;

        if (film.getMpa() != null && favoriteMpa.contains(film.getMpa().getId())) {
            score += 2;
        }

        score += Math.min(film.getLikes().size(), 5);

        log.debug("Оценка фильма id={}, name={}: {}", film.getId(), film.getName(), score);
        return score;
    }
}