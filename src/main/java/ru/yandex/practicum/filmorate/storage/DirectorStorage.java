package ru.yandex.practicum.filmorate.storage;

import java.util.List;

import ru.yandex.practicum.filmorate.model.Director;

public interface DirectorStorage {
    public List<Director> getAllDirectors();

    public Director getDirectorById(int id);

    public Director createDirector(Director director);

    public Director updateDirector(Director director);

    public void deleteDirectorById(int directorId);
}