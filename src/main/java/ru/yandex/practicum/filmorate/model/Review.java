package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @JsonProperty("reviewId")
    private Long id;

    @NotNull(message = "filmId обязателен")
    private Long filmId;

    @NotNull(message = "userId обязателен")
    private Long userId;

    @NotBlank(message = "Содержимое отзыва не может быть пустым")
    private String content;

    @NotNull(message = "isPositive обязателен")
    private Boolean isPositive;

    private Integer useful = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}