package ru.yandex.practicum.filmorate.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.DirectorStorage;

@Slf4j
@Service
@AllArgsConstructor
public class DirectorService {
    private final DirectorStorage directorStorage;

    public List<Director> getAllDirectors() {
        log.info("Запрос на получение всех режиссеров");
        List<Director> directors = directorStorage.getAllDirectors();
        log.info("Получено {} режиссеров", directors.size());
        return directors;
    }

    public Director getDirectorById(Integer directorId) {
        log.info("Запрос режиссера с id={}", directorId);
        Director director = directorStorage.getDirectorById(directorId)
                .orElseThrow(() -> new NotFoundException("Режиссер с id=" + directorId + " не найден"));
        log.info("Режиссер с id={} успешно найден", directorId);
        return director;
    }

    public Director createDirector(Director director) {
        log.info("Запрос на создание режиссера: name={}", director.getName());
        Director createdDirector = directorStorage.createDirector(director);
        log.info("Режиссер успешно создан с id={}", createdDirector.getId());
        return createdDirector;
    }

    public Director updateDirector(Director director) {
        log.info("Запрос на обновление режиссера id={}", director.getId());
        getDirectorById(director.getId());
        Director updatedDirector = directorStorage.updateDirector(director);
        log.info("Режиссер с id={} успешно обновлен", updatedDirector.getId());
        return updatedDirector;
    }

    public void deleteDirectorBy(int directorId) {
        log.info("Запрос на удаление режиссера с id={}", directorId);
        getDirectorById(directorId);
        directorStorage.deleteDirectorById(directorId);
        log.info("Режиссер с id={} успешно удален", directorId);
    }
}