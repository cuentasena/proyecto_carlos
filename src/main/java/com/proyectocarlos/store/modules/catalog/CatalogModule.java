package com.proyectocarlos.store.modules.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectocarlos.store.shared.http.HttpError;
import com.proyectocarlos.store.shared.events.EventBus;
import com.proyectocarlos.store.shared.http.Router;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CatalogModule {
  private final ProductRepository productRepository = new InMemoryProductRepository();
  private final CatalogService service = new CatalogService(productRepository);
  @SuppressWarnings("unused")
  private final EventBus eventBus;
  @SuppressWarnings("unused")
  private final ObjectMapper objectMapper;

  public CatalogModule(EventBus eventBus, ObjectMapper objectMapper) {
    this.eventBus = eventBus;
    this.objectMapper = objectMapper;
  }

  public void registerRoutes(Router router) {
    new CatalogController(service, router).registerRoutes();
  }

  // ===== Model =====
  public record Product(String id, String name, String brand, BigDecimal price) {}

  // ===== Repository =====
  public interface ProductRepository {
    Product save(Product product);
    List<Product> findAll();
    Optional<Product> findById(String id);
  }

  public static final class InMemoryProductRepository implements ProductRepository {
    private final ConcurrentHashMap<String, Product> data = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
      data.put(product.id(), product);
      return product;
    }

    @Override
    public List<Product> findAll() {
      return new ArrayList<>(data.values());
    }

    @Override
    public Optional<Product> findById(String id) {
      return Optional.ofNullable(data.get(id));
    }
  }

  // ===== Service =====
  public static final class CatalogService {
    private final ProductRepository productRepository;

    public CatalogService(ProductRepository productRepository) {
      this.productRepository = productRepository;
    }

    public Product createProduct(String name, String brand, BigDecimal price) {
      if (name == null || name.isBlank()) throw new IllegalArgumentException("name es requerido");
      if (brand == null || brand.isBlank()) throw new IllegalArgumentException("brand es requerido");
      if (price == null || price.signum() < 0) throw new IllegalArgumentException("price inválido");
      var p = new Product(UUID.randomUUID().toString(), name.trim(), brand.trim(), price);
      return productRepository.save(p);
    }

    public List<Product> listProducts() {
      return productRepository.findAll();
    }
  }

  // ===== Controller + DTOs =====
  public static final class CreateProductRequest {
    public String name;
    public String brand;
    public BigDecimal price;
  }

  public static final class CatalogController {
    private final CatalogService service;
    private final Router router;

    public CatalogController(CatalogService service, Router router) {
      this.service = service;
      this.router = router;
    }

    public void registerRoutes() {
      router.get("/catalog/products", exchange -> {
        try {
          router.writeJson(exchange, 200, service.listProducts());
        } catch (Exception e) {
          router.writeJson(exchange, 500, new HttpError(e.getMessage()));
        }
      });

      router.post("/catalog/products", exchange -> {
        try {
          var req = router.readJson(exchange, CreateProductRequest.class);
          var created = service.createProduct(req.name, req.brand, req.price);
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
  }
}

