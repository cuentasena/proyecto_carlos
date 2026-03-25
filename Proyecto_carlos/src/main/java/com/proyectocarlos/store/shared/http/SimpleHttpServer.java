package com.proyectocarlos.store.shared.http;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public final class SimpleHttpServer {
  private final Router router;
  private HttpServer server;

  public SimpleHttpServer(Router router) {
    this.router = router;
  }

  public void start(int port) throws IOException {
    server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", router.handler());
    server.setExecutor(Executors.newFixedThreadPool(8));
    server.start();
  }

  public void stop() {
    if (server != null) server.stop(0);
  }
}

