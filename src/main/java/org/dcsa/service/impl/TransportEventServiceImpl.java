package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.exception.NotFoundException;
import org.dcsa.model.TransportEvent;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.TransportEventRepository;
import org.dcsa.service.TransportEventService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TransportEventServiceImpl extends BaseServiceImpl<TransportEventRepository, TransportEvent, UUID> implements TransportEventService {
    private final TransportEventRepository transportEventRepository;

    @Override
    TransportEventRepository getRepository() {
        return transportEventRepository;
    }

    @Override
    public String getType() {
        return "TransportEvent";
    }

    //Overriding base method here, as it marks empty results as an error, meaning we can't use switchOnEmpty()
    @Override
    public Mono<TransportEvent> findById(UUID id) {
        return getRepository().findById(id);
    }

    public Flux<TransportEvent> findTransportEvents(List<EventType> eventType, String bookingReference, String equipmentReference) {
        if (!eventType.contains(EventType.TRANSPORT)) return Flux.empty(); // Return empty if TRANSPORT event type is not defined
        if (bookingReference!=null ) return Flux.empty(); //If bookingReference is defined, we return empty - since bookingReferences don't exist in transportEvents
        if (equipmentReference!=null ) return Flux.empty(); //If equipmentReference is defined, we return empty - since equipmentReferences don't exist in transportEvents

        return transportEventRepository.findTransportEventsByFilters(EventType.TRANSPORT);
    }

}
