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
    }

    /**
     * Получить рекомендации фильмов для пользователя на основе похожих пользователей
     * Алгоритм: Collaborative Filtering
     * 1. Находим пользователей с наибольшим пересечением лайков
     * 2. Рекомендуем фильмы, которые лайкнули похожие пользователи, но не лайкнул текущий
     */
    public RecommendationResponse getRecommendations(Long userId) {
        log.info("Получение рекомендаций для пользователя id={}", userId);

        // Проверяем существование пользователя
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        // Получаем все лайки
        Map<Long, List<Long>> allLikes = filmStorage.getAllLikes();

        // Фильмы, которые уже лайкнул пользователь
        Set<Long> userLikedFilms = getUserLikedFilms(userId, allLikes);

        if (userLikedFilms.isEmpty()) {
            log.info("У пользователя id={} нет лайков, возвращаем популярные фильмы", userId);
            List<Film> popularFilms = filmStorage.getPopular(10);
            return new RecommendationResponse(userId, popularFilms);
        }

        // Находим наиболее похожего пользователя
        Long mostSimilarUserId = findMostSimilarUser(userId, allLikes, userLikedFilms);

        if (mostSimilarUserId == null) {
            log.info("Для пользователя id={} не найдено похожих пользователей", userId);
            List<Film> popularFilms = filmStorage.getPopular(10);
            return new RecommendationResponse(userId, popularFilms);
        }

        log.debug("Наиболее похожий пользователь для id={}: {}", userId, mostSimilarUserId);

        // Получаем фильмы, которые лайкнул похожий пользователь, но не лайкнул текущий
        Set<Long> similarUserLikedFilms = getUserLikedFilms(mostSimilarUserId, allLikes);
        Set<Long> filmsToRecommend = new HashSet<>(similarUserLikedFilms);
        filmsToRecommend.removeAll(userLikedFilms);

        if (filmsToRecommend.isEmpty()) {
            log.info("Нет новых фильмов для рекомендации пользователю id={}", userId);
            List<Film> popularFilms = filmStorage.getPopular(10);
            return new RecommendationResponse(userId, popularFilms);
        }

        // Получаем полную информацию о фильмах
        List<Film> recommendations = filmsToRecommend.stream()
                .map(filmId -> filmStorage.getById(filmId).orElse(null))
                .filter(Objects::nonNull)
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(10)
                .collect(Collectors.toList());

        log.info("Найдено {} рекомендаций для пользователя id={}", recommendations.size(), userId);
        return new RecommendationResponse(userId, recommendations);
    }

    /**
     * Найти наиболее похожего пользователя по пересечению лайков
     * @param userId ID текущего пользователя
     * @param allLikes карта всех лайков
     * @param userLikedFilms фильмы, лайкнутые текущим пользователем
     * @return ID наиболее похожего пользователя или null, если такого нет
     */
    private Long findMostSimilarUser(Long userId, Map<Long, List<Long>> allLikes, Set<Long> userLikedFilms) {
        // Собираем обратную карту: пользователь -> лайкнутые фильмы
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

            // Находим пересечение лайков
            Set<Long> intersection = new HashSet<>(userLikedFilms);
            intersection.retainAll(otherUserLikedFilms);

            if (intersection.size() > maxIntersection) {
                maxIntersection = intersection.size();
                mostSimilarUserId = otherUserId;
            }
        }

        return mostSimilarUserId;
    }

    /**
     * Получить множество фильмов, лайкнутых пользователем
     */
    private Set<Long> getUserLikedFilms(Long userId, Map<Long, List<Long>> allLikes) {
        return allLikes.entrySet().stream()
                .filter(entry -> entry.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Альтернативный метод: рекомендации на основе жанров и рейтингов
     */
    public RecommendationResponse getRecommendationsByPreferences(Long userId) {
        log.info("Получение рекомендаций по предпочтениям для пользователя id={}", userId);

        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        // Получаем фильмы, которые пользователь уже лайкнул
        Map<Long, List<Long>> allLikes = filmStorage.getAllLikes();
        Set<Long> userLikedFilms = getUserLikedFilms(userId, allLikes);

        if (userLikedFilms.isEmpty()) {
            return getRecommendations(userId); // fallback к обычным рекомендациям
        }

        // Получаем информацию о лайкнутых фильмах
        List<Film> likedFilms = userLikedFilms.stream()
                .map(filmId -> filmStorage.getById(filmId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Анализируем предпочтения пользователя
        Set<Integer> favoriteGenres = getFavoriteGenres(likedFilms);
        Set<Integer> favoriteMpa = getFavoriteMpa(likedFilms);

        // Получаем фильмы, которые пользователь не лайкал
        List<Film> notLikedFilms = filmStorage.getFilmsNotLikedByUser(userId);

        // Сортируем по схожести с предпочтениями
        List<Film> recommendations = notLikedFilms.stream()
                .sorted((f1, f2) -> {
                    int score1 = calculateFilmScore(f1, favoriteGenres, favoriteMpa);
                    int score2 = calculateFilmScore(f2, favoriteGenres, favoriteMpa);
                    return Integer.compare(score2, score1);
                })
                .limit(10)
                .collect(Collectors.toList());

        log.info("Найдено {} рекомендаций по предпочтениям для пользователя id={}",
                recommendations.size(), userId);

        return new RecommendationResponse(userId, recommendations);
    }

    /**
     * Получить любимые жанры пользователя
     */
    private Set<Integer> getFavoriteGenres(List<Film> likedFilms) {
        return likedFilms.stream()
                .flatMap(film -> film.getGenres().stream())
                .map(Genre::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Получить любимые рейтинги MPA пользователя
     */
    private Set<Integer> getFavoriteMpa(List<Film> likedFilms) {
        return likedFilms.stream()
                .map(film -> film.getMpa())
                .filter(Objects::nonNull)
                .map(Mpa::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Рассчитать оценку фильма на основе предпочтений пользователя
     */
    private int calculateFilmScore(Film film, Set<Integer> favoriteGenres, Set<Integer> favoriteMpa) {
        int score = 0;

        // Баллы за совпадение жанров
        int genreMatches = (int) film.getGenres().stream()
                .map(Genre::getId)
                .filter(favoriteGenres::contains)
                .count();
        score += genreMatches * 3;

        // Баллы за совпадение MPA
        if (film.getMpa() != null && favoriteMpa.contains(film.getMpa().getId())) {
            score += 2;
        }

        // Бонус за популярность (количество лайков)
        score += Math.min(film.getLikes().size(), 5);

        return score;
    }
}