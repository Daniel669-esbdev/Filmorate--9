package ru.yandex.practicum.filmorate.storage;

import java.util.List;
import java.util.Optional;
import ru.yandex.practicum.filmorate.model.Director;

public interface DirectorStorage {
    List<Director> getAllDirectors();

    Optional<Director> getDirectorById(int id);

    Director createDirector(Director director);

    Director updateDirector(Director director);

    void deleteDirectorById(int directorId);
}