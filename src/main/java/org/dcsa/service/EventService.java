package org.dcsa.service;

import org.dcsa.model.Event;
import org.dcsa.model.Events;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface EventService extends BaseService<Event, UUID>{

    Flux<Event> findAll();

    <T extends Event> Mono<T> findAnyById(UUID id);

    Mono<Events> findAllWrapped(Flux<Event> events);

    <T extends Event> Flux<T> findAllTypes(List<EventType> eventType, String bookingReference, String equipmentReference);

    <T extends Event> Mono<T> saveAll(Event event);
}
