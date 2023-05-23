package com.chaottic.artifact.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

public final class ArtifactManager implements HttpHandler {
    private final Path artifactsPath;

    private final String username;
    private final String password;

    public ArtifactManager(Path artifactsPath, String username, String password) {
        this.artifactsPath = artifactsPath;
        this.username = username;
        this.password = password;
    }

    public void getFileOrDirectory(HttpExchange httpExchange) throws IOException {
        var uriPath = httpExchange.getRequestURI().getPath();

        var path = artifactsPath.resolve(uriPath.substring(uriPath.indexOf("/") + 1));

        var builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><head><style>h1 { font-family: \"Montserrat Medium\", Arial, sans-serif; } p { font-family: \"Lato\", Arial, sans-serif;}</style></head><body><h1>").append(path).append("</h1>");

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
                builder.append("<p><a href=").append(Paths.get(uriPath).getParent()).append(">").append(".../").append("</a></p>");
            }

            try (var stream = Files.list(path)) {
                stream.forEachOrdered(child -> {
                    var displayName = child.getFileName().toString();

                    displayName = Files.isDirectory(child) ? "%s/".formatted(displayName) : displayName;

                    var href = Paths.get(uriPath).resolve(child.getFileName());

                    builder.append("<p><a href=").append(href).append(">").append(displayName).append("</a></p>");
                });
            }

            builder.append("</body></html>");

            var response = builder.toString();
            httpExchange.sendResponseHeaders(200, response.length());

            try (var outputStream = httpExchange.getResponseBody()) {
                outputStream.write(response.getBytes());
                outputStream.flush();
            }
        } else {
            builder.append("<p>404</p>").append("</body></html>");

            var response = builder.toString();
            httpExchange.sendResponseHeaders(404, response.length());

            try (var outputStream = httpExchange.getResponseBody()) {
                outputStream.write(response.getBytes());
                outputStream.flush();
            }
        }
    }

    public void createAuthorizedFileOrDirectories(HttpExchange httpExchange) throws IOException {
        var headers = httpExchange.getRequestHeaders();
        if (headers.containsKey("Authorization")) {
            var credentials = new String(Base64.getDecoder().decode(headers.getFirst("Authorization").substring(6))).split(":", 2);

            if (credentials[0].equals(username) && credentials[1].equals(password)) {
                var path = artifactsPath.resolve(httpExchange.getRequestURI().getPath().substring(1));

                Files.createDirectories(path);

                try (var inputStream = httpExchange.getRequestBody()) {
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                }

                var response = "Successfully published artifact.";
                httpExchange.sendResponseHeaders(200, response.length());
                try (var outputStream = httpExchange.getResponseBody()) {
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                }
                return;
            }
        }

        httpExchange.sendResponseHeaders(401, -1);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "PUT" -> createAuthorizedFileOrDirectories(exchange);
            case "GET" -> getFileOrDirectory(exchange);
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }
}
