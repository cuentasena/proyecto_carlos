package com.proyectocarlos.store.shared.contracts;

import com.proyectocarlos.store.shared.events.DomainEvent;

public final class StockRejectedEvent implements DomainEvent {
  public final String orderId;
  public final String reason;

  public StockRejectedEvent(String orderId, String reason) {
    this.orderId = orderId;
    this.reason = reason;
  }
}

