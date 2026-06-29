package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson = new Gson();

    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String[] pathParts = path.split("/");
        boolean hasId = pathParts.length > 2 && !pathParts[2].isEmpty();

        if (method.equalsIgnoreCase("GET")) {
            if (hasId) {
                try {
                    handleGetById(exchange, pathParts[2]);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                handleGetAll(exchange);
            }
        } else if (method.equalsIgnoreCase("POST")) {
            handlePost(exchange);
        } else if (method.equalsIgnoreCase("DELETE")) {
            if (hasId) {
                handleDelete(exchange, pathParts[2]);
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.getResponseBody().close();
        }
    }

    private void handleDelete(HttpExchange exchange, String idStr) throws IOException {
        try {
            int id = Integer.parseInt(idStr);
            Movie deletedMovie = store.remove(id);

            if (deletedMovie == null) {
                ErrorResponse error = new ErrorResponse("Фильм не найден");
                String errorJson = gson.toJson(error);
                sendJson(exchange, 404, errorJson);
            } else {
                sendNoContent(exchange);
            }
        } catch (NumberFormatException e) {
            ErrorResponse error = new ErrorResponse("Некорректный ID");
            String errorJson = gson.toJson(error);
            sendJson(exchange, 400, errorJson);
        }
    }

    private void handleGetAll(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.startsWith("year=")) {
            String yearParam = query.substring(5);
            if (yearParam.isEmpty()) {
                ErrorResponse error = new ErrorResponse("Некорректный параметр запроса — 'year'");
                String errorJson = gson.toJson(error);
                sendJson(exchange, 400, errorJson);
                return;
            }

            try {
                int year = Integer.parseInt(yearParam);
                List<Movie> allMovies = store.getAll();
                List<Movie> filteredMovies = allMovies.stream()
                        .filter(movie -> movie.getYear() == year)
                        .toList();

                String response = gson.toJson(filteredMovies);
                sendJson(exchange, 200, response);
            } catch (NumberFormatException e) {
                ErrorResponse error = new ErrorResponse("Некорректный параметр запроса — 'year'");
                String errorJson = gson.toJson(error);
                sendJson(exchange, 400, errorJson);
            }
        } else {
            List<Movie> movies = store.getAll();
            String response = gson.toJson(movies);
            sendJson(exchange, 200, response);
        }
    }

    private void handleGetById(HttpExchange exchange, String idStr) throws Exception {
        try {
            int id = Integer.parseInt(idStr);
            Movie movie = store.getById(id);

            if (movie == null) {
                ErrorResponse error = new ErrorResponse("Фильм не найден");
                String errorJson = gson.toJson(error);
                sendJson(exchange, 404, errorJson);
            } else {
                String response = gson.toJson(movie);
                sendJson(exchange, 200, response);
            }
        } catch (NumberFormatException e) {
            ErrorResponse error = new ErrorResponse("Некорректный ID");
            String errorJson = gson.toJson(error);
            sendJson(exchange, 400, errorJson);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            exchange.sendResponseHeaders(415, -1);
            exchange.getResponseBody().close();
            return;
        }
        try {
            InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            Movie newMovie = gson.fromJson(reader, Movie.class);

            List<String> validationErrors = validateMovie(newMovie);

            if (!validationErrors.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", validationErrors);
                String errorJson = gson.toJson(errorResponse);
                sendJson(exchange, 422, errorJson);
                return;
            }

            Movie saveMovie = store.add(newMovie);

            String response = gson.toJson(saveMovie);
            sendJson(exchange, 201, response);
        } catch (Exception e) {
            exchange.sendResponseHeaders(500, -1);
            exchange.getResponseBody().close();
        }
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            errors.add("название не должно превышать " + MAX_TITLE_LENGTH + " символов");
        }

        int currentYear = Year.now().getValue();
        int maxYear = currentYear + 1;

        if (movie.getYear() < MIN_YEAR) {
            errors.add("год должен быть не меньше " + MIN_YEAR);
        } else if (movie.getYear() > maxYear) {
            errors.add("год должен быть не больше " + maxYear);
        }

        return errors;
    }

}
