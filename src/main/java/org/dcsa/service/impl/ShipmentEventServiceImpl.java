package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.base.service.impl.ExtendedBaseServiceImpl;
import org.dcsa.model.ShipmentEvent;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.ShipmentEventRepository;
import org.dcsa.service.ShipmentEventService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class ShipmentEventServiceImpl extends ExtendedBaseServiceImpl<ShipmentEventRepository, ShipmentEvent, UUID> implements ShipmentEventService {
    private final ShipmentEventRepository shipmentEventRepository;

    @Override
    public ShipmentEventRepository getRepository() {
        return shipmentEventRepository;
    }

    @Override
    public Class<ShipmentEvent> getModelClass() {
        return ShipmentEvent.class;
    }

    //Overriding base method here, as it marks empty results as an error, meaning we can't use switchOnEmpty()
    @Override
    public Mono<ShipmentEvent> findById(UUID id) {
        return getRepository().findById(id);
    }

    public Flux<ShipmentEvent> findShipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference) {
        // Return empty if SHIPMENT event type is not defined
        if (!eventType.contains(EventType.SHIPMENT)) return Flux.empty();
        // If bookingReference is defined, we return empty - since bookingReferences don't exist in shipmentEvents
        if (bookingReference!=null ) return Flux.empty();
        // If equipmentReference is defined, we return empty - since equipmentReferences don't exist in shipmentEvents
        if (equipmentReference!=null ) return Flux.empty();
        return shipmentEventRepository.findShipmentEventsByFilters(EventType.SHIPMENT);
    }
}
