package com.chaottic.artifact.manager;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Paths;

public final class Main {

    private Main() {}

    public static void main(String[] args) throws IOException, ParseException {
        var options = new Options();
        options.addOption("username", true, "");
        options.addOption("password", true, "");

        var parser = new DefaultParser();
        var cli = parser.parse(options, args);

        var username = cli.getOptionValue("username");
        var password = cli.getOptionValue("password");

        var server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new ArtifactManager(Paths.get("artifacts"), username, password));
        server.setExecutor(null);
        server.start();
    }
}
