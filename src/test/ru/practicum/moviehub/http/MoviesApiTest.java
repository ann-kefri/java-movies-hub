package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        gson = new Gson();
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMoviesWhenEmptyReturnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"));
    }

    @Test
    void postMoviesValidReturnsCreated() throws Exception {
        String json = "{\"title\":\"Интерстеллар\",\"year\":2014}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());

        assertEquals(1, store.size());
        Movie saveMovie = store.getById(1);
        assertEquals("Интерстеллар", saveMovie.getTitle());
    }

    @Test
    void postMoviesTitleEmpty() throws Exception {
        String json = "{\"title\":\"\",\"year\":2020}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("error"));
    }

    @Test
    void postMoviesTitleTooLong() throws Exception {
        String longTitle = "a".repeat(101);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":2020}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMoviesYearTooOld() throws Exception {
        String json = "{\"title\":\"Старый фильм\",\"year\":1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMoviesWrongContentType() throws Exception {
        String json = "{\"title\":\"Интерстеллар\",\"year\":2014}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")  // Неправильный Content-Type
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
    }

    @Test
    void getMoviesByIdWhenExist() throws Exception {
        String json = "{\"title\":\"Интерстеллар\",\"year\":2014}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        Movie movie = gson.fromJson(resp.body(), Movie.class);
        assertEquals(1, movie.getId());
        assertEquals("Интерстеллар", movie.getTitle());
        assertEquals(2014, movie.getYear());
    }

    @Test
    void getMovieByIdNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void getMoviesByIdInvalidId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void deleteMovieByIdExist() throws Exception {
        String json = "{\"title\":\"Интерстеллар\",\"year\":2014}";
        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(1, store.size());

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(deleteReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode());
        assertEquals(0, store.size());
        assertNull(store.getById(1));
    }

    @Test
    void deleteMoviesByIdNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void deleteMovieByIdInvalidId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void getMoviesByYearExist() throws Exception {
        addMovie("Интерстеллар", 2014);
        addMovie("Начало", 2010);
        addMovie("Дюна", 2021);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2014"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        Movie[] movies = gson.fromJson(resp.body(), Movie[].class);
        assertEquals(1, movies.length);
        assertEquals("Интерстеллар", movies[0].getTitle());
        assertEquals(2014, movies[0].getYear());
    }

    @Test
    void getMoviesByYearNoMovies() throws Exception {
        addMovie("Начало", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        Movie[] movies = gson.fromJson(resp.body(), Movie[].class);
        assertEquals(0, movies.length);
    }

    @Test
    void getMovieByYearInvalidYear() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abcd"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Некорректный параметр запроса"));
    }

    private void addMovie(String title, int year) throws Exception {
        String json = "{\"title\":\"" + title + "\",\"year\":" + year + "}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}