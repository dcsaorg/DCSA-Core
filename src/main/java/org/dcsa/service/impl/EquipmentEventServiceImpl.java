package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.model.EquipmentEvent;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.EquipmentEventRepository;
import org.dcsa.service.EquipmentEventService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class EquipmentEventServiceImpl extends ExtendedBaseServiceImpl<EquipmentEventRepository, EquipmentEventRepository, EquipmentEvent, UUID> implements EquipmentEventService {

    private final EquipmentEventRepository equipmentEventRepository;

    @Override
    EquipmentEventRepository getRepository() {
        return equipmentEventRepository;
    }

    @Override
    public Class<EquipmentEvent> getModelClass() {
        return EquipmentEvent.class;
    }

    //Overriding base method here, as it marks empty results as an error, meaning we can't use switchOnEmpty()
    @Override
    public Mono<EquipmentEvent> findById(UUID id) {
        return getRepository().findById(id);
    }

    public Flux<EquipmentEvent> findEquipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference) {
        // Return empty if EQUIPMENT event type is not defined
        if (!eventType.contains(EventType.EQUIPMENT)) return Flux.empty();
        //If bookingReference is defined, we return empty - since bookingReferences don't exist in equipmentEvents
        if (bookingReference!=null ) return Flux.empty();
        return equipmentEventRepository.findAllEquipmentEventsByFilters(EventType.EQUIPMENT, null, equipmentReference);
    }
}
