package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.model.EquipmentEvent;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.EquipmentEventRepository;
import org.dcsa.service.EquipmentEventService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@RequiredArgsConstructor
@Service
public class EquipmentEventServiceImpl extends BaseServiceImpl<EquipmentEventRepository, EquipmentEvent, String> implements EquipmentEventService {
    private final EquipmentEventRepository equipmentEventRepository;


    @Override
    EquipmentEventRepository getRepository() {
        return equipmentEventRepository;
    }

    @Override
    public String getType() {
        return "EquipmentEvent";
    }


    public Flux<EquipmentEvent> findEquipmentEvents(List<EventType> eventType, String bookingReference, String equipmentReference) {

        if (!eventType.contains(EventType.EQUIPMENT)) return Flux.empty(); // Return empty if EQUIPMENT event type is not defined
        if (bookingReference!=null ) return Flux.empty(); //If bookingReference is defined, we return empty - since bookingReferences don't exist in equipmentEvents

        return equipmentEventRepository.findAllEquipmentEventsByFilters(EventType.EQUIPMENT, null, equipmentReference);
    }
}
