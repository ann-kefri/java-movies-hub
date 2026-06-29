package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    public Movie add(Movie movie) {
        movie.setId(nextId++);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public Movie getById(int id) {
        return movies.get(id);
    }

    public Movie remove(int id) {
        return movies.remove(id);
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }

    public boolean isEmpty() {
        return movies.isEmpty();
    }

    public int size() {
        return movies.size();
    }
}