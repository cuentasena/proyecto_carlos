package com.proyectocarlos.store.shared.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Router {
  private final ObjectMapper objectMapper;
  private final Map<String, Map<String, HttpHandler>> routes = new ConcurrentHashMap<>();

  public Router(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void get(String path, HttpHandler handler) {
    register("GET", path, handler);
  }

  public void post(String path, HttpHandler handler) {
    register("POST", path, handler);
  }

  public HttpHandler handler() {
    return exchange -> {
      var method = exchange.getRequestMethod().toUpperCase();
      if ("OPTIONS".equals(method)) {
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return;
      }
      var path = exchange.getRequestURI().getPath();
      var methodRoutes = routes.get(path);
      if (methodRoutes == null || !methodRoutes.containsKey(method)) {
        writeJson(exchange, 404, new HttpError("No existe ruta: " + method + " " + path));
        return;
      }
      methodRoutes.get(method).handle(exchange);
    };
  }

  public <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
    try (InputStream in = exchange.getRequestBody()) {
      return objectMapper.readValue(in, type);
    }
  }

  public void addCorsHeaders(HttpExchange exchange) {
    var h = exchange.getResponseHeaders();
    h.set("Access-Control-Allow-Origin", "*");
    h.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    h.set("Access-Control-Allow-Headers", "Content-Type");
  }

  public void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
    byte[] bytes = objectMapper.writeValueAsBytes(body);
    addCorsHeaders(exchange);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  public void writeText(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    addCorsHeaders(exchange);
    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private void register(String method, String path, HttpHandler handler) {
    routes.computeIfAbsent(path, ignored -> new ConcurrentHashMap<>()).put(method, handler);
  }
}

