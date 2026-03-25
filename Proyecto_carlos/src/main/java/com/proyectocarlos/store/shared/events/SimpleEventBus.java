package com.proyectocarlos.store.shared.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SimpleEventBus implements EventBus {
  private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();

  @Override
  public <T extends DomainEvent> void publish(T event) {
    if (event == null) return;
    var list = handlers.get(event.getClass());
    if (list == null) return;
    for (var handler : list) {
      @SuppressWarnings("unchecked")
      var typed = (EventHandler<T>) handler;
      typed.handle(event);
    }
  }

  @Override
  public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
    handlers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(handler);
  }
}

