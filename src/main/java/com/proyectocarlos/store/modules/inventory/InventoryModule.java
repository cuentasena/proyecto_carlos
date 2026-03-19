package com.proyectocarlos.store.modules.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectocarlos.store.shared.contracts.OrderPlacedEvent;
import com.proyectocarlos.store.shared.contracts.StockRejectedEvent;
import com.proyectocarlos.store.shared.contracts.StockReservedEvent;
import com.proyectocarlos.store.shared.events.EventBus;
import com.proyectocarlos.store.shared.http.HttpError;
import com.proyectocarlos.store.shared.http.Router;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryModule {
  private final InventoryRepository repository = new InMemoryInventoryRepository();
  private final InventoryService service = new InventoryService(repository);

  private final EventBus eventBus;
  @SuppressWarnings("unused")
  private final ObjectMapper objectMapper;

  public InventoryModule(EventBus eventBus, ObjectMapper objectMapper) {
    this.eventBus = eventBus;
    this.objectMapper = objectMapper;
    wireSubscriptions();
  }

  public void registerRoutes(Router router) {
    new InventoryController(service, router).registerRoutes();
  }

  private void wireSubscriptions() {
    eventBus.subscribe(OrderPlacedEvent.class, this::onOrderPlaced);
  }

  private void onOrderPlaced(OrderPlacedEvent evt) {
    boolean reserved = service.reserveForOrder(evt);
    if (reserved) {
      eventBus.publish(new StockReservedEvent(evt.orderId));
    } else {
      eventBus.publish(new StockRejectedEvent(evt.orderId, "Stock insuficiente para uno o más productos"));
    }
  }

  // ===== Model =====
  public record StockItem(String productId, int quantity) {}

  // ===== Repository =====
  public interface InventoryRepository {
    void addStock(String productId, int quantity);
    Optional<StockItem> findByProductId(String productId);
    List<StockItem> findAll();
    boolean tryReserve(OrderPlacedEvent order);
  }

  public static final class InMemoryInventoryRepository implements InventoryRepository {
    private final ConcurrentHashMap<String, Integer> stock = new ConcurrentHashMap<>();

    @Override
    public void addStock(String productId, int quantity) {
      if (productId == null || productId.isBlank()) throw new IllegalArgumentException("productId es requerido");
      if (quantity <= 0) throw new IllegalArgumentException("quantity debe ser > 0");
      stock.merge(productId, quantity, Integer::sum);
    }

    @Override
    public Optional<StockItem> findByProductId(String productId) {
      Integer q = stock.get(productId);
      if (q == null) return Optional.empty();
      return Optional.of(new StockItem(productId, q));
    }

    @Override
    public List<StockItem> findAll() {
      var out = new ArrayList<StockItem>();
      for (var e : stock.entrySet()) out.add(new StockItem(e.getKey(), e.getValue()));
      return out;
    }

    @Override
    public synchronized boolean tryReserve(OrderPlacedEvent order) {
      for (var line : order.lines) {
        int available = stock.getOrDefault(line.productId, 0);
        if (available < line.quantity) return false;
      }
      for (var line : order.lines) {
        stock.put(line.productId, stock.getOrDefault(line.productId, 0) - line.quantity);
      }
      return true;
    }
  }

  // ===== Service =====
  public static final class InventoryService {
    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
      this.repository = repository;
    }

    public void addStock(String productId, int quantity) {
      repository.addStock(productId, quantity);
    }

    public List<StockItem> listStock() {
      return repository.findAll();
    }

    public boolean reserveForOrder(OrderPlacedEvent orderPlaced) {
      return repository.tryReserve(orderPlaced);
    }
  }

  // ===== Controller + DTOs =====
  public static final class AddStockRequest {
    public String productId;
    public int quantity;
  }

  public static final class InventoryController {
    private final InventoryService service;
    private final Router router;

    public InventoryController(InventoryService service, Router router) {
      this.service = service;
      this.router = router;
    }

    public void registerRoutes() {
      router.get("/inventory/stock", exchange -> {
        try {
          router.writeJson(exchange, 200, service.listStock());
        } catch (Exception e) {
          router.writeJson(exchange, 500, new HttpError(e.getMessage()));
        }
      });

      router.post("/inventory/stock/add", exchange -> {
        try {
          var req = router.readJson(exchange, AddStockRequest.class);
          service.addStock(req.productId, req.quantity);
          router.writeJson(exchange, 200, service.listStock());
        } catch (IllegalArgumentException e) {
          router.writeJson(exchange, 400, new HttpError(e.getMessage()));
        } catch (IOException e) {
          router.writeJson(exchange, 400, new HttpError("JSON inválido"));
        } catch (Exception e) {
          router.writeJson(exchange, 500, new HttpError(e.getMessage()));
        }
      });
    }
  }
}

