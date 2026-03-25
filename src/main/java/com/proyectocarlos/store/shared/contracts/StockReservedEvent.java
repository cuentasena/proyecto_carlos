package com.proyectocarlos.store.shared.contracts;

import com.proyectocarlos.store.shared.events.DomainEvent;

public final class StockReservedEvent implements DomainEvent {
  public final String orderId;

  public StockReservedEvent(String orderId) {
    this.orderId = orderId;
  }
}

