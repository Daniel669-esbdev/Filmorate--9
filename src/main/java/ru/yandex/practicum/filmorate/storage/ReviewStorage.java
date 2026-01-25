    package ru.yandex.practicum.filmorate.storage;

    import ru.yandex.practicum.filmorate.model.Review;

    import java.util.List;
    import java.util.Optional;

    public interface ReviewStorage {

        Review create(Review review);

        Review update(Review review);

        boolean delete(Long reviewId);

        Optional<Review> findById(Long id);

        List<Review> findByFilmId(Long filmId, Integer count);

        void addLike(Long reviewId, Long userId);

        void removeLike(Long reviewId, Long userId);

        void addDislike(Long reviewId, Long userId);

        void removeDislike(Long reviewId, Long userId);
    }