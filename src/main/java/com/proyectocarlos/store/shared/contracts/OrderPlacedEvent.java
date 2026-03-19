package com.proyectocarlos.store.shared.contracts;

import com.proyectocarlos.store.shared.events.DomainEvent;
import java.util.List;

public final class OrderPlacedEvent implements DomainEvent {
  public final String orderId;
  public final List<Line> lines;

  public OrderPlacedEvent(String orderId, List<Line> lines) {
    this.orderId = orderId;
    this.lines = lines;
  }

  public static final class Line {
    public final String productId;
    public final int quantity;

    public Line(String productId, int quantity) {
      this.productId = productId;
      this.quantity = quantity;
    }
  }
}

