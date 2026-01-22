package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.service.EventService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class EventController {

    private final EventService eventService;

    @GetMapping("/{id}/feed")
    public List<Event> getUserFeed(@PathVariable Long id) {
        log.info("Получение ленты событий для пользователя с id={}", id);
        List<Event> events = eventService.getEventsByUserId(id);
        log.debug("Возвращено {} событий для пользователя с id={}", events.size(), id);
        return events;
    }
}