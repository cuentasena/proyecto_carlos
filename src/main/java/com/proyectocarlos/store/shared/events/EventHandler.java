package com.proyectocarlos.store.shared.events;

@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
  void handle(T event);
}

