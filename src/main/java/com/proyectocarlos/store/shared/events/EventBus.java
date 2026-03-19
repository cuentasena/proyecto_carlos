package com.proyectocarlos.store.shared.events;

public interface EventBus {
  <T extends DomainEvent> void publish(T event);
  <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler);
}

