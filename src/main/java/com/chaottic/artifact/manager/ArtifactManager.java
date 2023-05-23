package com.chaottic.artifact.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ArtifactManager implements HttpHandler {
    private final Path artifactsPath;

    public ArtifactManager(Path artifactsPath) {
        this.artifactsPath = artifactsPath;
    }

    public void getFileOrDirectory(HttpExchange httpExchange) throws IOException {
        var uriPath = httpExchange.getRequestURI().getPath();

        var path = artifactsPath.resolve(uriPath.substring(uriPath.indexOf("/") + 1));

        var builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><h1>").append(path).append("</h1>");

        if (Files.isRegularFile(path)) {
            httpExchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"%s\"".formatted(path.getFileName()));
            httpExchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            httpExchange.sendResponseHeaders(200, Files.size(path));

            try (var inputStream = Files.newInputStream(path); var outputStream = httpExchange.getResponseBody()) {
                var buffer = new byte[16*16*16];
                var read = 0;

                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
            }
        } else if (Files.isDirectory(path)) {
            var parent = path.getParent();
            if (parent != null) {
                builder.append("<p><a href=").append(Paths.get(uriPath).getParent()).append(">").append("...\\").append("</a></p>");
            }

            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    var displayName = child.getFileName().toString();

                    displayName = Files.isDirectory(child) ? "%s\\".formatted(displayName) : displayName;

                    var href = Paths.get(uriPath).resolve(child.getFileName());

                    builder.append("<p><a href=").append(href).append(">").append(displayName).append("</a></p>");
                });
            }

            builder.append("</html>");

            var response = builder.toString();
            httpExchange.sendResponseHeaders(200, response.length());

            try (var outputStream = httpExchange.getResponseBody()) {
                outputStream.write(response.getBytes());
                outputStream.flush();
            }
        } else {
            builder.append("<p>404</p>").append("</html>");

            var response = builder.toString();
            httpExchange.sendResponseHeaders(404, response.length());

            try (var outputStream = httpExchange.getResponseBody()) {
                outputStream.write(response.getBytes());
                outputStream.flush();
            }
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "GET" -> {
                getFileOrDirectory(exchange);
            }
            default -> exchange.sendResponseHeaders(405, 0);
        }
    }
}