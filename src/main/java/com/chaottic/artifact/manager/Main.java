package com.chaottic.artifact.manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;

public final class Main {

    private Main() {}

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        var server = HttpServer.create(new InetSocketAddress(8080), 0);

        try (var executor = Executors.newCachedThreadPool()) {
            server.createContext("/maven", new Handler(getSha256(args[0].substring(13))));
            server.setExecutor(executor);
            server.start();
        }
    }

    private static byte[] getSha256(String credentials) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(credentials.getBytes());
    }

    private static final class Handler implements HttpHandler {
        private final byte[] sha256;

        private Handler(byte[] sha256) {
            this.sha256 = sha256;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            switch (exchange.getRequestMethod()) {
                case "GET" -> {

                }
                case "PUT" -> {
                    authorized(exchange);

                    try (var outputStream = exchange.getResponseBody()) {
                        outputStream.write("".getBytes());
                        outputStream.flush();
                    }
                }
                default -> exchange.sendResponseHeaders(405, -1);
            }
        }

        private void authorized(HttpExchange exchange) {
            var headers = exchange.getRequestHeaders();
            if (headers.containsKey("Authorization")) {
                return;
            }
        }
    }
}
