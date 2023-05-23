package com.chaottic.artifact.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {
    private static final Path ARTIFACTS_PATH = Paths.get("artifacts");

    private static final Path ROOT = Paths.get("/");

    private Main() {}

    public static void main(String[] args) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new ExchangeHandler("test", "test"));
        server.setExecutor(null);
        server.start();
    }

    private record ExchangeHandler(String username, String password) implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            switch (exchange.getRequestMethod()) {
                case "PUT" -> {
                    var headers = exchange.getRequestHeaders();
                    if (headers.containsKey("Authorization")) {
                        System.out.println(headers.getFirst("Authorization"));
                    }

                    exchange.sendResponseHeaders(401, -1);
                }
                case "GET" -> {
                    getFileOrDirectory(exchange);
                }
                default -> exchange.sendResponseHeaders(405, 0);
            }
        }

        private void getFileOrDirectory(HttpExchange httpExchange) throws IOException {
            var path = getPath(httpExchange.getRequestURI().getPath());

            var builder = new StringBuilder();
            builder.append("<!DOCTYPE html><html><h1>").append(path).append("</h1>");

            if (path.getParent() != null) {
                builder.append("<p><a href=").append(ROOT.resolve(path.getParent())).append(">").append("...\\").append("</a></p>");
            }

            if (Files.isRegularFile(path)) {

            } else if (Files.isDirectory(path)) {
                try (var list = Files.list(path)) {
                    list.forEach(child -> {
                        var fileName = child.getFileName();

                        builder.append("<p><a href=").append(ROOT.resolve(child)).append(">").append(Files.isDirectory(path) ? "%s\\".formatted(fileName) : fileName).append("</a></p>");
                    });
                }
            } else {
                builder.append("<p>404</p>");
            }

            var response = builder.append("</html>").toString();

            httpExchange.getResponseHeaders().set("Content-Type", "text/html");
            httpExchange.sendResponseHeaders(200, response.length());

            try (var outputStream = httpExchange.getResponseBody()) {
                outputStream.write(response.getBytes());
                outputStream.flush();
            }
        }

        private Path getPath(String path) {
            if (path.equals("/")) {
               return ARTIFACTS_PATH;
            }

            return Paths.get(path.substring(path.indexOf("/") + 1));
        }
    }
}
