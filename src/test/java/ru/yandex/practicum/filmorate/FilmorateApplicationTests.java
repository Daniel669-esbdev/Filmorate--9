/*package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmorateApplicationTests {

	private FilmService filmService;
	private InMemoryFilmStorage filmStorage;

	@BeforeEach
	void setUp() {
		filmStorage = new InMemoryFilmStorage();
		filmService = new FilmService(filmStorage);
	}

	@Test
	void addAndDeleteLike() {
		Film film = new Film();
		film.setName("Test Film");
		film.setDescription("Desc");
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(120);

		film = filmService.create(film);

		filmService.addLike(film.getId(), 1L);
		filmService.addLike(film.getId(), 2L);

		Film updatedFilm = filmService.getById(film.getId());
		assertEquals(2, updatedFilm.getLikes().size());
		assertTrue(updatedFilm.getLikes().contains(1L));
		assertTrue(updatedFilm.getLikes().contains(2L));

		filmService.deleteLike(film.getId(), 1L);

		updatedFilm = filmService.getById(film.getId());
		assertEquals(1, updatedFilm.getLikes().size());
		assertFalse(updatedFilm.getLikes().contains(1L));
	}

	@Test
	void getPopularFilms() {
		Film film1 = createFilm("Film 1");
		Film film2 = createFilm("Film 2");

		filmService.addLike(film1.getId(), 1L);
		filmService.addLike(film1.getId(), 2L);
		filmService.addLike(film2.getId(), 1L);

		List<Film> popular = filmService.getPopular(10);

		assertEquals(2, popular.size());
		assertEquals(film1.getId(), popular.get(0).getId());
		assertEquals(film2.getId(), popular.get(1).getId());
	}

	private Film createFilm(String name) {
		Film film = new Film();
		film.setName(name);
		film.setDescription("Desc");
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(120);
		return filmService.create(film);
	}
} */