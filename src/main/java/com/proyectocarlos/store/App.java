package com.proyectocarlos.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectocarlos.store.modules.catalog.CatalogModule;
import com.proyectocarlos.store.modules.inventory.InventoryModule;
import com.proyectocarlos.store.modules.orders.OrdersModule;
import com.proyectocarlos.store.shared.events.SimpleEventBus;
import com.proyectocarlos.store.shared.http.Router;
import com.proyectocarlos.store.shared.http.SimpleHttpServer;

public final class App {
  public static void main(String[] args) throws Exception {
    var objectMapper = new ObjectMapper();
    var eventBus = new SimpleEventBus();
    var router = new Router(objectMapper);

    var catalogModule = new CatalogModule(eventBus, objectMapper);
    var inventoryModule = new InventoryModule(eventBus, objectMapper);
    var ordersModule = new OrdersModule(eventBus, objectMapper);

    catalogModule.registerRoutes(router);
    inventoryModule.registerRoutes(router);
    ordersModule.registerRoutes(router);

    var http = new SimpleHttpServer(router);
    http.start(8080);

    System.out.println("Cosmetics Store (Modular Monolith) listo en http://localhost:8080");
    System.out.println("Endpoints:");
    System.out.println("- GET  /catalog/products");
    System.out.println("- POST /catalog/products");
    System.out.println("- GET  /inventory/stock");
    System.out.println("- POST /inventory/stock/add");
    System.out.println("- GET  /orders");
    System.out.println("- POST /orders");
  }
}
