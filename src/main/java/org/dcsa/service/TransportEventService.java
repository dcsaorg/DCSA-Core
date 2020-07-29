package org.dcsa.service;

import org.dcsa.model.TransportEvent;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface TransportEventService extends BaseService<TransportEvent, UUID> {

    Flux<TransportEvent> findAll();

    Mono<TransportEvent> findById(UUID id);

    Flux<TransportEvent> findTransportEvents(List<EventType> eventType, String bookingReference, String equipmentReference);
}
