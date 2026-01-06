package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmorateApplicationTests {

	@Test
	void shouldCreateValidFilm() {
		Film film = new Film();
		film.setName("Valid Film");
		film.setDescription("Good description");
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(120);

		assertDoesNotThrow(() -> validate(film));
	}

	@Test
	void shouldThrowWhenNameIsEmpty() {
		Film film = new Film();
		film.setName("");
		film.setDescription("Desc");
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(120);

		ValidationException ex = assertThrows(ValidationException.class, () -> validate(film));
		assertTrue(ex.getMessage().contains("Название"));
	}

	@Test
	void shouldThrowWhenDescriptionTooLong() {
		Film film = new Film();
		film.setName("Film");
		film.setDescription("a".repeat(201));
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(120);

		ValidationException ex = assertThrows(ValidationException.class, () -> validate(film));
		assertTrue(ex.getMessage().contains("200 символов"));
	}

	@Test
	void shouldThrowWhenReleaseDateTooEarly() {
		Film film = new Film();
		film.setName("Old Film");
		film.setDescription("Desc");
		film.setReleaseDate(LocalDate.of(1950, 12, 27));
		film.setDuration(120);

		ValidationException ex = assertThrows(ValidationException.class, () -> validate(film));
		assertTrue(ex.getMessage().contains("28 декабря 1950"));
	}

	@Test
	void shouldThrowWhenDurationNegative() {
		Film film = new Film();
		film.setName("Film");
		film.setDescription("Desc");
		film.setReleaseDate(LocalDate.of(2000, 1, 1));
		film.setDuration(-100);

		ValidationException ex = assertThrows(ValidationException.class, () -> validate(film));
		assertTrue(ex.getMessage().contains("положительной"));
	}

	private void validate(Film film) {
		if (film.getName() == null || film.getName().isBlank()) {
			throw new ValidationException("Название не может быть пустым");
		}
		if (film.getDescription() != null && film.getDescription().length() > 200) {
			throw new ValidationException("Описание не может превышать 200 символов");
		}
		if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(LocalDate.of(1950, 12, 28))) {
			throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1950 года");
		}
		if (film.getDuration() <= 0) {
			throw new ValidationException("Продолжительность должна быть положительной");
		}
	}
}