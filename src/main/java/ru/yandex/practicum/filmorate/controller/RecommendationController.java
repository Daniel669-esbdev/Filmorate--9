package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.RecommendationResponse;
import ru.yandex.practicum.filmorate.service.RecommendationService;

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<?> getRecommendations(@PathVariable Long id) {
        log.info("Получен запрос рекомендаций для пользователя id={}", id);

        try {
            RecommendationResponse response = recommendationService.getRecommendations(id);
            log.info("Успешно возвращены рекомендации для пользователя id={}", id);
            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            log.warn("Пользователь не найден: id={}, error={}", id, e.getMessage());
            return createErrorResponse(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());

        } catch (IllegalArgumentException e) {
            log.warn("Некорректный запрос: id={}, error={}", id, e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());

        } catch (RuntimeException e) {
            // 500 - Внутренняя ошибка сервера
            log.error("Внутренняя ошибка сервера при запросе рекомендаций пользователя id={}: {}",
                    id, e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error", "Произошла внутренняя ошибка сервера");

        } catch (Exception e) {
            log.error("Неожиданная ошибка при запросе рекомендаций пользователя id={}: {}",
                    id, e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error", "Произошла непредвиденная ошибка");
        }
    }

    @GetMapping("/{id}/recommendations/preferences")
    public ResponseEntity<?> getRecommendationsByPreferences(@PathVariable Long id) {
        log.info("Получен запрос рекомендаций по предпочтениям для пользователя id={}", id);

        try {
            RecommendationResponse response = recommendationService.getRecommendationsByPreferences(id);
            log.info("Успешно возвращены рекомендации по предпочтениям для пользователя id={}", id);
            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            log.warn("Пользователь не найден (preferences): id={}, error={}", id, e.getMessage());
            return createErrorResponse(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());

        } catch (IllegalArgumentException e) {
            log.warn("Некорректный запрос (preferences): id={}, error={}", id, e.getMessage());
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());

        } catch (RuntimeException e) {
            log.error("Внутренняя ошибка сервера при запросе рекомендаций по предпочтениям пользователя id={}: {}",
                    id, e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error", "Произошла внутренняя ошибка сервера");

        } catch (Exception e) {
            log.error("Неожиданная ошибка при запросе рекомендаций по предпочтениям пользователя id={}: {}",
                    id, e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error", "Произошла непредвиденная ошибка");
        }
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status, String error, String message) {

        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("status", status.value());

        return ResponseEntity.status(status).body(response);
    }
}