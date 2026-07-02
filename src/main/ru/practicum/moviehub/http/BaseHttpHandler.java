package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.getResponseBody().close();
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(204, -1);
        exchange.getResponseBody().close();
    }
}