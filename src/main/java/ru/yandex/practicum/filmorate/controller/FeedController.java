package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/users")
public class FeedController {

    private final EventService eventService;

    @GetMapping("/{id}/feed")
    public List<Event> getUserFeed(@PathVariable Long id) {
        log.info("Получен запрос на ленту событий пользователя id={}", id);
        try {
            List<Event> feed = eventService.getUserFeed(id);
            log.info("Успешно возвращено {} событий для пользователя id={}", feed.size(), id);
            return feed;
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса ленты событий пользователя id={}: {}",
                    id, e.getMessage());
            throw e;
        }
    }
}