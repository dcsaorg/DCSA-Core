package org.dcsa.service;

import org.dcsa.model.Event;
import org.dcsa.model.Events;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface EventService extends BaseService<Event, UUID>{
    Mono<Events> findAllWrapped(Flux<Event> events);
    Flux<Event> findAllTypes(List<EventType> eventType, String bookingReference, String equipmentReference);
}
