package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.RecommendationResponse;
import ru.yandex.practicum.filmorate.service.RecommendationService;

@RestController
@RequestMapping("/users")
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
        log.info("RecommendationController инициализирован");
    }

    @GetMapping("/{id}/recommendations")
    public RecommendationResponse getRecommendations(@PathVariable Long id) {
        log.info("Получен запрос рекомендаций для пользователя id={}", id);
        try {
            RecommendationResponse response = recommendationService.getRecommendations(id);
            log.info("Успешно возвращены рекомендации для пользователя id={}", id);
            return response;
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса рекомендаций пользователя id={}: {}",
                    id, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/{id}/recommendations/preferences")
    public RecommendationResponse getRecommendationsByPreferences(@PathVariable Long id) {
        log.info("Получен запрос рекомендаций по предпочтениям для пользователя id={}", id);
        try {
            RecommendationResponse response = recommendationService.getRecommendationsByPreferences(id);
            log.info("Успешно возвращены рекомендации по предпочтениям для пользователя id={}", id);
            return response;
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса рекомендаций по предпочтениям пользователя id={}: {}",
                    id, e.getMessage());
            throw e;
        }
    }
}