package com.proyectocarlos.store.modules.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectocarlos.store.shared.contracts.OrderPlacedEvent;
import com.proyectocarlos.store.shared.contracts.StockRejectedEvent;
import com.proyectocarlos.store.shared.contracts.StockReservedEvent;
import com.proyectocarlos.store.shared.events.EventBus;
import com.proyectocarlos.store.shared.http.HttpError;
import com.proyectocarlos.store.shared.http.Router;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OrdersModule {
  private final OrderRepository repository = new InMemoryOrderRepository();
  private final OrderService service;
  private final EventBus eventBus;
  @SuppressWarnings("unused")
  private final ObjectMapper objectMapper;

  public OrdersModule(EventBus eventBus, ObjectMapper objectMapper) {
    this.eventBus = eventBus;
    this.objectMapper = objectMapper;
    this.service = new OrderService(repository, eventBus);
    wireSubscriptions();
  }

  public void registerRoutes(Router router) {
    new OrdersController(service, router).registerRoutes();
  }

  private void wireSubscriptions() {
    eventBus.subscribe(StockReservedEvent.class, evt -> service.confirmOrder(evt.orderId));
    eventBus.subscribe(StockRejectedEvent.class, evt -> service.rejectOrder(evt.orderId, evt.reason));
  }

  // ===== Model =====
  public enum OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED
  }

  public record OrderItem(String productId, int quantity) {}

  public static final class Order {
    public final String id;
    public final Instant createdAt;
    public volatile OrderStatus status;
    public volatile String rejectionReason;
    public final List<OrderItem> items;

    public Order(String id, Instant createdAt, OrderStatus status, List<OrderItem> items) {
      this.id = id;
      this.createdAt = createdAt;
      this.status = status;
      this.items = items;
    }

    public void confirm() {
      this.status = OrderStatus.CONFIRMED;
      this.rejectionReason = null;
    }

    public void reject(String reason) {
      this.status = OrderStatus.REJECTED;
      this.rejectionReason = reason;
    }
  }

  // ===== Repository =====
  public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String id);
    List<Order> findAll();
  }

  public static final class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentHashMap<String, Order> data = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
      data.put(order.id, order);
      return order;
    }

    @Override
    public Optional<Order> findById(String id) {
      return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Order> findAll() {
      return new ArrayList<>(data.values());
    }
  }

  // ===== Service =====
  public static final class OrderService {
    private final OrderRepository repository;
    private final EventBus eventBus;

    public OrderService(OrderRepository repository, EventBus eventBus) {
      this.repository = repository;
      this.eventBus = eventBus;
    }

    public Order placeOrder(List<OrderItem> items) {
      if (items == null || items.isEmpty()) throw new IllegalArgumentException("items es requerido");
      for (var i : items) {
        if (i.productId() == null || i.productId().isBlank()) throw new IllegalArgumentException("productId es requerido");
        if (i.quantity() <= 0) throw new IllegalArgumentException("quantity debe ser > 0");
      }

      var order = new Order(UUID.randomUUID().toString(), Instant.now(), OrderStatus.PENDING, items);
      repository.save(order);

      var lines = items.stream()
          .map(i -> new OrderPlacedEvent.Line(i.productId(), i.quantity()))
          .toList();
      eventBus.publish(new OrderPlacedEvent(order.id, lines));

      return order;
    }

    public List<Order> listOrders() {
      return repository.findAll();
    }

    public void confirmOrder(String orderId) {
      repository.findById(orderId).ifPresent(Order::confirm);
    }

    public void rejectOrder(String orderId, String reason) {
      repository.findById(orderId).ifPresent(o -> o.reject(reason));
    }
  }

  // ===== Controller + DTOs =====
  public static final class PlaceOrderRequest {
    public List<Line> items;

    public static final class Line {
      public String productId;
      public int quantity;
    }
  }

  public static final class OrdersController {
    private final OrderService service;
    private final Router router;

    public OrdersController(OrderService service, Router router) {
      this.service = service;
      this.router = router;
    }

    public void registerRoutes() {
      router.get("/orders", exchange -> {
        try {
          router.writeJson(exchange, 200, service.listOrders());
        } catch (Exception e) {
          router.writeJson(exchange, 500, new HttpError(e.getMessage()));
        }
      });

      router.post("/orders", exchange -> {
        try {
          var req = router.readJson(exchange, PlaceOrderRequest.class);
          var items = toItems(req);
          var created = service.placeOrder(items);
          router.writeJson(exchange, 201, created);
        } catch (IllegalArgumentException e) {
          router.writeJson(exchange, 400, new HttpError(e.getMessage()));
        } catch (IOException e) {
          router.writeJson(exchange, 400, new HttpError("JSON inválido"));
        } catch (Exception e) {
          router.writeJson(exchange, 500, new HttpError(e.getMessage()));
        }
      });
    }

    private static List<OrderItem> toItems(PlaceOrderRequest req) {
      if (req == null || req.items == null) throw new IllegalArgumentException("items es requerido");
      return req.items.stream()
          .map(i -> new OrderItem(i.productId, i.quantity))
          .toList();
    }
  }
}

