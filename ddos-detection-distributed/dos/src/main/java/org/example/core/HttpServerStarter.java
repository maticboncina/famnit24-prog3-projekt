package org.example.core;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerStarter {
    public static void startHttpServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", SharedData.handler);
        server.createContext("/index.html", SharedData.handler);
        server.setExecutor(SharedData.httpExecutor);
        server.start();
    }
}
