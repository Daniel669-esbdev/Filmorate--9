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
        try {
            if (!userStorage.existsById(userId)) {
                String errorMsg = String.format("Пользователь с id=%d не найден", userId);
                log.error(errorMsg);
                throw new NotFoundException(errorMsg);
            }

            Map<Long, List<Long>> allLikes = filmStorage.getAllLikes();
            Set<Long> userLikedFilms = getUserLikedFilms(userId, allLikes);

            if (userLikedFilms.isEmpty()) {
                log.info("У пользователя id={} нет лайков, возвращаем популярные фильмы", userId);
                List<Film> popularFilms = filmStorage.getPopular(10);
                RecommendationResponse response = new RecommendationResponse(userId, popularFilms);
                log.info("Рекомендации (популярные фильмы) успешно сформированы для пользователя id={}", userId);
                return response;
            }

            Long mostSimilarUserId = findMostSimilarUser(userId, allLikes, userLikedFilms);

            if (mostSimilarUserId == null) {
                log.info("Для пользователя id={} не найдено похожих пользователей", userId);
                List<Film> popularFilms = filmStorage.getPopular(10);
                RecommendationResponse response = new RecommendationResponse(userId, popularFilms);
                log.info("Рекомендации (популярные фильмы) успешно сформированы для пользователя id={}", userId);
                return response;
            }

            log.info("Наиболее похожий пользователь для id={}: {}", userId, mostSimilarUserId);

            Set<Long> similarUserLikedFilms = getUserLikedFilms(mostSimilarUserId, allLikes);
            Set<Long> filmsToRecommend = new HashSet<>(similarUserLikedFilms);
            filmsToRecommend.removeAll(userLikedFilms);

            if (filmsToRecommend.isEmpty()) {
                log.info("Нет новых фильмов для рекомендации пользователю id={}", userId);
                List<Film> popularFilms = filmStorage.getPopular(10);
                RecommendationResponse response = new RecommendationResponse(userId, popularFilms);
                log.info("Рекомендации (популярные фильмы) успешно сформированы для пользователя id={}", userId);
                return response;
            }

            List<Film> recommendations = filmsToRecommend.stream()
                    .map(filmId -> filmStorage.getById(filmId).orElse(null))
                    .filter(Objects::nonNull)
                    .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                    .limit(10)
                    .collect(Collectors.toList());

            RecommendationResponse response = new RecommendationResponse(userId, recommendations);
            log.info("Успешно сформировано {} рекомендаций для пользователя id={}",
                    recommendations.size(), userId);
            return response;
        } catch (NotFoundException e) {
            log.error("Ошибка при получении рекомендаций: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при получении рекомендаций для пользователя id={}: {}",
                    userId, e.getMessage());
            throw new RuntimeException("Не удалось получить рекомендации", e);
        }
    }

    private Long findMostSimilarUser(Long userId, Map<Long, List<Long>> allLikes, Set<Long> userLikedFilms) {
        log.debug("Поиск наиболее похожего пользователя для userId={}", userId);
        try {
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
        } catch (Exception e) {
            log.error("Ошибка при поиске похожего пользователя: {}", e.getMessage());
            return null;
        }
    }

    private Set<Long> getUserLikedFilms(Long userId, Map<Long, List<Long>> allLikes) {
        log.debug("Получение лайкнутых фильмов пользователя id={}", userId);
        try {
            return allLikes.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(userId))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Ошибка при получении лайкнутых фильмов пользователя id={}: {}",
                    userId, e.getMessage());
            return Collections.emptySet();
        }
    }

    public RecommendationResponse getRecommendationsByPreferences(Long userId) {
        log.info("Начало получения рекомендаций по предпочтениям для пользователя id={}", userId);
        try {
            if (!userStorage.existsById(userId)) {
                String errorMsg = String.format("Пользователь с id=%d не найден", userId);
                log.error(errorMsg);
                throw new NotFoundException(errorMsg);
            }

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

            RecommendationResponse response = new RecommendationResponse(userId, recommendations);
            log.info("Успешно сформировано {} рекомендаций по предпочтениям для пользователя id={}",
                    recommendations.size(), userId);
            return response;
        } catch (NotFoundException e) {
            log.error("Ошибка при получении рекомендаций по предпочтениям: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Неожиданная ошибка при получении рекомендаций по предпочтениям для пользователя id={}: {}",
                    userId, e.getMessage());
            throw new RuntimeException("Не удалось получить рекомендации по предпочтениям", e);
        }
    }

    private Set<Integer> getFavoriteGenres(List<Film> likedFilms) {
        log.debug("Получение любимых жанров из {} фильмов", likedFilms.size());
        try {
            return likedFilms.stream()
                    .flatMap(film -> film.getGenres().stream())
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Ошибка при получении любимых жанров: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private Set<Integer> getFavoriteMpa(List<Film> likedFilms) {
        log.debug("Получение любимых рейтингов MPA из {} фильмов", likedFilms.size());
        try {
            return likedFilms.stream()
                    .map(film -> film.getMpa())
                    .filter(Objects::nonNull)
                    .map(Mpa::getId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Ошибка при получении любимых рейтингов MPA: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private int calculateFilmScore(Film film, Set<Integer> favoriteGenres, Set<Integer> favoriteMpa) {
        try {
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
        } catch (Exception e) {
            log.error("Ошибка при расчете оценки фильма id={}: {}", film.getId(), e.getMessage());
            return 0;
        }
    }
}