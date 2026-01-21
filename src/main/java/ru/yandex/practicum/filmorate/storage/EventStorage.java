package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Event;
import java.util.List;

public interface EventStorage {
    Event create(Event event);

    List<Event> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteAll();
}