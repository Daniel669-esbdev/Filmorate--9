package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Review {
    @PositiveOrZero
    private Long id;

    @NotNull(message = "filmId обязателен")
    @Positive(message = "filmId должен быть положительным")
    private Long filmId;

    @NotNull(message = "userId обязателен")
    @Positive(message = "userId должен быть положительным")
    private Long userId;

    @NotBlank(message = "Содержимое отзыва не может быть пустым")
    @Size(max = 2000, message = "Отзыв не может быть длиннее 2000 символов")
    private String content;

    @NotNull(message = "isPositive обязателен")
    private Boolean isPositive;

    private Integer useful = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}