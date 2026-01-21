package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private final Map<Long, Set<Long>> likes = new HashMap<>(); // filmId -> Set<userId>
    private long idCounter = 1;

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public Film create(Film film) {
        film.setId(idCounter++);
        films.put(film.getId(), film);
        likes.put(film.getId(), new HashSet<>()); // Инициализируем пустой список лайков
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
    public List<Film> getPopular(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                    int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                    return Integer.compare(likes2, likes1); // Сортируем по убыванию лайков
                })
                .limit(count > 0 ? count : 10)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<Long>> getAllLikes() {
        Map<Long, List<Long>> result = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entry : likes.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
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
    public List<Film> getFilmsWithFilter(Map<String, String> params) {

        if (params == null || params.isEmpty()) {
            return new ArrayList<>(films.values());
        }

        if (params.containsKey("count")) {
            try {
                int count = Integer.parseInt(params.get("count"));
                return new ArrayList<>(films.values()).stream()
                        .limit(count)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
            }
        }

        return new ArrayList<>(films.values());
    }
}