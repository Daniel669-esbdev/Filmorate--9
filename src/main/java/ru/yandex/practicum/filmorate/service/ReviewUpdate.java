package ru.yandex.practicum.filmorate.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ReviewUpdate {

    @NotNull(message = "id обязателен")
    @Positive(message = "id должен быть положительным")
    @JsonProperty("reviewId")
    private Long id;

    @NotBlank(message = "Содержимое отзыва не может быть пустым")
    private String content;

    @NotNull(message = "isPositive обязателен")
    private Boolean isPositive;
}