package com.chaottic.artifact.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;

public final class Main {
    private static final Path ARTIFACTS_PATH = Paths.get("artifacts");

    private Main() {}

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        var server = HttpServer.create(new InetSocketAddress(8080), 0);

        try (var executor = Executors.newCachedThreadPool()) {
            var contextPath = "/maven";

            server.createContext(contextPath, new Handler(getSha256(args[0].substring(13)), contextPath));
            server.setExecutor(executor);
            server.start();
        }
    }

    private static byte[] getSha256(String credentials) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(credentials.getBytes());
    }

    private static final class Handler implements HttpHandler {
        private final byte[] sha256;
        private final String contextPath;

        private Handler(byte[] sha256, String contextPath) {
            this.sha256 = sha256;
            this.contextPath = contextPath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            switch (exchange.getRequestMethod()) {
                case "GET" -> {

                }
                case "PUT" -> {
                    authorized(exchange);

                    var artifactDetails = exchange.getRequestURI().getPath().substring(contextPath.length() + 1);
                    var path = ARTIFACTS_PATH.resolve(artifactDetails);

                    Files.createDirectories(path);

                    try (var inputStream = exchange.getRequestBody()) {
                        Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                default -> exchange.sendResponseHeaders(405, -1);
            }
        }

        private void authorized(HttpExchange exchange) throws IOException {
            var headers = exchange.getRequestHeaders();
            if (headers.containsKey("Authorization")) {
                return;
            }

            exchange.sendResponseHeaders(401, -1);
        }
    }
}
