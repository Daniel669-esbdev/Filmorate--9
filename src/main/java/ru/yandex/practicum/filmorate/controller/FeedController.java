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
        log.info("Запрос ленты событий пользователя id={}", id);
        List<Event> feed = eventService.getUserFeed(id);
        log.debug("Возвращено {} событий для пользователя id={}", feed.size(), id);
        return feed;
    }
}