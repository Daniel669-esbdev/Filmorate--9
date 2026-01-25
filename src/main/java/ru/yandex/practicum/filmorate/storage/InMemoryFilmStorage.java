package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private final Map<Long, Set<Long>> likes = new HashMap<>();
    private long idCounter = 1;

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public Film create(Film film) {
        film.setId(idCounter++);
        films.put(film.getId(), film);
        likes.put(film.getId(), new HashSet<>());
        return film;
    }

    @Override
    public Film update(Film film) {
        if (!films.containsKey(film.getId())) {
            throw new ru.yandex.practicum.filmorate.exception.NotFoundException(
                    "Фильм с id=" + film.getId() + " не найден");
        }
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Optional<Film> getById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public List<Film> findAllByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .map(films::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFilm(Long id) {
        films.remove(id);
        likes.remove(id);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        if (!films.containsKey(filmId)) {
            throw new ru.yandex.practicum.filmorate.exception.NotFoundException(
                    "Фильм с id=" + filmId + " не найден");
        }
        likes.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        if (likes.containsKey(filmId)) {
            likes.get(filmId).remove(userId);
        }
    }

    @Override
    public List<Film> getPopular(int count, Integer genreId, Integer year) {
        return films.values().stream()
                .filter(film -> year == null || film.getReleaseDate().getYear() == year)
                .filter(film -> genreId == null || (film.getGenres() != null && film.getGenres().stream()
                        .anyMatch(g -> g.getId().equals(genreId))))
                .sorted((f1, f2) -> {
                    int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                    int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                    return Integer.compare(likes2, likes1);
                })
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
                            film.getDirectors() != null && film.getDirectors().stream()
                            .anyMatch(d -> d.getName().toLowerCase().contains(lowerQuery));
                    return matchTitle || matchDirector;
                })
                .sorted((f1, f2) -> {
                    int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                    int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                    return Integer.compare(likes2, likes1);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> findAllBy(Integer directorId, String sortBy) {
        return films.values().stream()
                .filter(film -> film.getDirectors() != null && film.getDirectors().stream()
                        .anyMatch(d -> d.getId().equals(directorId)))
                .sorted((f1, f2) -> {
                    if ("likes".equals(sortBy)) {
                        int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                        int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                        return Integer.compare(likes2, likes1);
                    } else {
                        return f1.getReleaseDate().compareTo(f2.getReleaseDate());
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<Long>> getAllLikes() {
        Map<Long, List<Long>> userLikesMap = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entry : likes.entrySet()) {
            Long filmId = entry.getKey();
            for (Long userId : entry.getValue()) {
                userLikesMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(filmId);
            }
        }
        return userLikesMap;
    }

    @Override
    public List<Film> getFilmsNotLikedByUser(Long userId) {
        return films.values().stream()
                .filter(film -> {
                    Set<Long> filmLikes = likes.getOrDefault(film.getId(), Collections.emptySet());
                    return !filmLikes.contains(userId);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        return films.values().stream()
                .filter(film -> {
                    Set<Long> filmLikes = likes.getOrDefault(film.getId(), Collections.emptySet());
                    return filmLikes.contains(userId) && filmLikes.contains(friendId);
                })
                .sorted((f1, f2) -> {
                    int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                    int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                    return Integer.compare(likes2, likes1);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> getFilmsWithFilter(Map<String, String> params) {
        return new ArrayList<>(films.values());
    }
}