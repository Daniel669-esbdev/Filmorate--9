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
    }

    @GetMapping("/{id}/recommendations")
    public RecommendationResponse getRecommendations(@PathVariable Long id) {
        log.info("Запрос рекомендаций для пользователя id={}", id);
        return recommendationService.getRecommendations(id);
    }

    @GetMapping("/{id}/recommendations/preferences")
    public RecommendationResponse getRecommendationsByPreferences(@PathVariable Long id) {
        log.info("Запрос рекомендаций по предпочтениям для пользователя id={}", id);
        return recommendationService.getRecommendationsByPreferences(id);
    }
}