package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.model.*;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.EventRepository;
import org.dcsa.repository.EventSubscriptionRepository;
import org.dcsa.service.EventService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@Service
public class EventServiceImpl extends BaseServiceImpl<EventRepository, Event, String> implements EventService {
    private final EventRepository eventRepository;
    private final ShipmentEventServiceImpl shipmentEventService;
    private final TransportEventServiceImpl transportEventService;
    private final TransportEquipmentEventServiceImpl transportEquipmentEventService;
    private final EquipmentEventServiceImpl equipmentEventService;
    private final EventSubscriptionRepository eventSubscriptionRepository;


    @Override
    EventRepository getRepository() {
        return eventRepository;
    }

    @Override
    public String getType() {
        return "Event";
    }

    @Override
    public Mono<Event> findById(String id) {
        return super.findById(id);
    }

    @Override
    public <T extends Event> Flux<T> findAllTypes(List<EventType> eventType, String bookingReference, String equipmentReference) {
        return Flux.merge(
                (Flux<T>) shipmentEventService.findShipmentEvents(eventType, bookingReference, equipmentReference),
                (Flux<T>) transportEventService.findTransportEvents(eventType, bookingReference, equipmentReference),
                (Flux<T>) transportEquipmentEventService.findTransportEquipmentEvents(eventType, bookingReference, equipmentReference),
                (Flux<T>) equipmentEventService.findEquipmentEvents(eventType, bookingReference, equipmentReference)
        );
    }

    @Override
    public <T extends Event> Mono<T> saveAll(Event event) {
        Mono<T> returnEvent;
        Flux<String> callbackUrls;

        switch (event.getEventType()) {
            case "SHIPMENT":
                returnEvent = (Mono<T>) shipmentEventService.save((ShipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), null);
                break;
            case "TRANSPORT":
                returnEvent = (Mono<T>) transportEventService.save((TransportEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), null);
                break;
            case "TRANSPORTEQUIPMENT":
                returnEvent = (Mono<T>) transportEquipmentEventService.save((TransportEquipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), ((TransportEquipmentEvent) event).getEquipmentReference());
                break;
            case "EQUIPMENT":
                returnEvent = (Mono<T>) equipmentEventService.save((EquipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), ((EquipmentEvent) event).getEquipmentReference());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event.getEventType());
        }
        //Check all subscriptions
        callbackUrls.toStream().forEach(callbackUrl -> System.out.println("POST "+ callbackUrl));
        return returnEvent;
    }
}
