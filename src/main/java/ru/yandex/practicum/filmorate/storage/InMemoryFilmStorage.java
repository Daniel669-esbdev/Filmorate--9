package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long idCounter = 1;

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public Film create(Film film) {
        film.setId(idCounter++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Optional<Film> getById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public void deleteFilm(Long id) {
    }


    @Override
    public void addLike(Long filmId, Long userId) {
        if (films.containsKey(filmId)) {
            films.get(filmId).getLikes().add(userId);
        }
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        if (films.containsKey(filmId)) {
            films.get(filmId).getLikes().remove(userId);
        }
    }

    @Override
    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        return films.values().stream()
                .filter(film -> year == null || film.getReleaseDate().getYear() == year)
                .filter(film -> genreId == null || film.getGenres().stream()
                        .anyMatch(g -> g.getId() == genreId))
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> search(String query, String by) {
        String lowerQuery = query.toLowerCase();
        return films.values().stream()
                .filter(film -> {
                    boolean matchTitle = by.contains("title") &&
                            film.getName().toLowerCase().contains(lowerQuery);
                    boolean matchDirector = by.contains("director") &&
                            film.getDirectors().stream()
                                    .anyMatch(d -> d.getName().toLowerCase().contains(lowerQuery));
                    return matchTitle || matchDirector;
                })
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> findAllBy(Integer directorId, String sortBy) {
        return films.values().stream()
                .filter(film -> film.getDirectors().stream()
                        .anyMatch(d -> d.getId().equals(directorId)))
                .sorted((f1, f2) -> {
                    if ("likes".equals(sortBy)) {
                        return Integer.compare(f2.getLikes().size(), f1.getLikes().size());
                    } else {
                        return f1.getReleaseDate().compareTo(f2.getReleaseDate());
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        return List.of();
    }
}