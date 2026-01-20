package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import java.util.List;

@Data
public class RecommendationResponse {
    private Long userId;
    private List<Film> recommendations;
    private String message;

    public RecommendationResponse(Long userId, List<Film> recommendations) {
        this.userId = userId;
        this.recommendations = recommendations;
        if (recommendations.isEmpty()) {
            this.message = "Пока нет рекомендаций на основе ваших предпочтений";
        } else {
            this.message = String.format("Найдено %d рекомендаций на основе похожих пользователей", recommendations.size());
        }
    }
}