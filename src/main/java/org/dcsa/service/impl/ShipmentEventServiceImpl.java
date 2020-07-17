package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.model.ShipmentEvent;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.ShipmentEventRepository;
import org.dcsa.service.ShipmentEventService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;


@RequiredArgsConstructor
@Service
public class ShipmentEventServiceImpl extends BaseServiceImpl<ShipmentEventRepository, ShipmentEvent, String> implements ShipmentEventService {
    private final ShipmentEventRepository shipmentEventRepository;

    @Override
    ShipmentEventRepository getRepository() {
        return shipmentEventRepository;
    }

    @Override
    public String getType() {
        return "ShipmentEvent";
    }

    public Flux<ShipmentEvent> findShipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference) {
        if (!eventType.contains(EventType.SHIPMENT)) return Flux.empty(); // Return empty if SHIPMENT event type is not defined
        if (bookingReference!=null ) return Flux.empty(); //If bookingReference is defined, we return empty - since bookingReferences don't exist in shipmentEvents
        if (equipmentReference!=null ) return Flux.empty(); //If equipmentReference is defined, we return empty - since equipmentReferences don't exist in shipmentEvents
        return shipmentEventRepository.findShipmentEventsByFilters(EventType.SHIPMENT);
    }
}
