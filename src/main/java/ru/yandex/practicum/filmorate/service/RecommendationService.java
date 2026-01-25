package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {
    private final FilmStorage filmStorage;
    private static final int MAX_SIMILAR_USERS = 10;

    public List<Film> getRecommendations(Long userId) {
        log.info("Начат процесс поиска рекомендаций для пользователя с id={}", userId);

        Map<Long, List<Long>> allLikes = filmStorage.getAllLikes();

        if (allLikes.isEmpty() || !allLikes.containsKey(userId)) {
            log.info("Для пользователя id={} рекомендации отсутствуют (нет лайков)", userId);
            return Collections.emptyList();
        }

        List<Long> targetUserLikes = allLikes.get(userId);

        List<Long> similarUserIds = allLikes.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(userId))
                .map(entry -> {
                    long commonLikesCount = targetUserLikes.stream()
                            .filter(filmId -> entry.getValue().contains(filmId))
                            .count();
                    return Map.entry(entry.getKey(), commonLikesCount);
                })
                .filter(entry -> entry.getValue() > 0)
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(MAX_SIMILAR_USERS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (similarUserIds.isEmpty()) {
            log.info("Не найдено пользователей с похожими вкусами для id={}", userId);
            return Collections.emptyList();
        }

        Set<Long> recommendedFilmIds = similarUserIds.stream()
                .flatMap(id -> allLikes.get(id).stream())
                .filter(filmId -> !targetUserLikes.contains(filmId))
                .collect(Collectors.toSet());

        log.info("Для пользователя id={} найдено {} похожих пользователей. Сформировано {} рекомендаций.",
                userId, similarUserIds.size(), recommendedFilmIds.size());

        if (recommendedFilmIds.isEmpty()) {
            return Collections.emptyList();
        }

        return filmStorage.findAllByIds(recommendedFilmIds);
    }
}