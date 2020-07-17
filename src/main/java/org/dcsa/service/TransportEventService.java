package org.dcsa.service;

import org.dcsa.model.TransportEvent;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TransportEventService extends BaseService<TransportEvent, String> {

    Flux<TransportEvent> findAll();

    Mono<TransportEvent> findById(String id);

    Flux<TransportEvent> findTransportEvents(List<EventType> eventType, String bookingReference, String equipmentReference);
}
