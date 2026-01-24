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

    public List<Film> getRecommendations(Long userId) {
        log.info("Начат процесс поиска рекомендаций для пользователя с id={}", userId);

        Map<Long, List<Long>> allLikes = filmStorage.getAllLikes();

        if (allLikes.isEmpty() || !allLikes.containsKey(userId)) {
            log.info("Для пользователя id={} рекомендации отсутствуют (нет лайков)", userId);
            return Collections.emptyList();
        }

        List<Long> targetUserLikes = allLikes.get(userId);
        long bestMatchUserId = -1;
        int maxCommonLikes = 0;

        for (Map.Entry<Long, List<Long>> entry : allLikes.entrySet()) {
            long otherUserId = entry.getKey();
            if (otherUserId == userId) continue;

            List<Long> otherUserLikes = entry.getValue();
            int commonLikesCount = (int) targetUserLikes.stream()
                    .filter(otherUserLikes::contains)
                    .count();

            if (commonLikesCount > maxCommonLikes) {
                maxCommonLikes = commonLikesCount;
                bestMatchUserId = otherUserId;
            }
        }

        if (bestMatchUserId == -1) {
            log.info("Не найдено пользователей с похожими вкусами для id={}", userId);
            return Collections.emptyList();
        }

        List<Long> bestMatchUserLikes = allLikes.get(bestMatchUserId);
        List<Long> recommendedFilmIds = bestMatchUserLikes.stream()
                .filter(filmId -> !targetUserLikes.contains(filmId))
                .collect(Collectors.toList());

        log.info("Пользователь id={} наиболее похож на пользователя id={}. Найдено {} рекомендаций.",
                userId, bestMatchUserId, recommendedFilmIds.size());

        return recommendedFilmIds.stream()
                .map(filmStorage::getById)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }
}