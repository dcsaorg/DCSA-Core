package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.model.TransportEquipmentEvent;
import org.dcsa.model.TransportEvent;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.TransportEquipmentEventRepository;
import org.dcsa.service.TransportEquipmentEventService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TransportEquipmentEventServiceImpl extends BaseServiceImpl<TransportEquipmentEventRepository, TransportEquipmentEvent, UUID> implements TransportEquipmentEventService {
    private final TransportEquipmentEventRepository transportEquipmentEventRepository;

    @Override
    TransportEquipmentEventRepository getRepository() {
        return transportEquipmentEventRepository;
    }

    @Override
    public String getType() {
        return "TransportEquipmentEvent";
    }

    //Overriding base method here, as it marks empty results as an error, meaning we can't use switchOnEmpty()
    @Override
    public Mono<TransportEquipmentEvent> findById(UUID id) {
        return getRepository().findById(id);
    }
    public Flux<TransportEquipmentEvent> findTransportEquipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference) {
        if (!eventType.contains(EventType.TRANSPORTEQUIPMENT)) return Flux.empty(); // Return empty if TRANSPORTEQUIPMENT event type is not defined
        if (bookingReference!=null ) return Flux.empty(); //If bookingReference is defined, we return empty - since bookingReferences don't exist in equipmentEvents

        return transportEquipmentEventRepository.findTransportEquipmentEventsByFilters(EventType.TRANSPORTEQUIPMENT, null, equipmentReference);
    }
}
