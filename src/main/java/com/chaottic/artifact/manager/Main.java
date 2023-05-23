package com.chaottic.artifact.manager;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;

public final class Main {

    private Main() {}

    public static void main(String[] args) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new ArtifactManager(Paths.get("artifacts")));
        server.setExecutor(null);
        server.start();
    }
}
