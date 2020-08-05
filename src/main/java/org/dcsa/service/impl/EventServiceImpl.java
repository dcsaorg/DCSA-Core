package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.exception.NotFoundException;
import org.dcsa.model.*;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.EventRepository;
import org.dcsa.repository.EventSubscriptionRepository;
import org.dcsa.service.EventService;
import org.dcsa.util.CallbackHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class EventServiceImpl extends BaseServiceImpl<EventRepository, Event, UUID> implements EventService {
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
    public Mono<Event> findById(UUID id) {
        return eventRepository.findById(UUID.randomUUID())
                .switchIfEmpty(transportEventService.findById(id))
                .switchIfEmpty(shipmentEventService.findById(id))
                .switchIfEmpty(transportEquipmentEventService.findById(id))
                .switchIfEmpty(equipmentEventService.findById(id))
                .switchIfEmpty(Mono.error(new NotFoundException("No event was found with id: " + id)));
    }

    @Override
    public Mono<Events> findAllWrapped(Flux<Event> events) {
        return events.collectList().map(Events::new);
    }

    @Override
    public Flux<Event> findAllTypes(List<EventType> eventType, String bookingReference, String equipmentReference) {
        return Flux.merge(
                shipmentEventService.findShipmentEvents(eventType, bookingReference, equipmentReference),
                transportEventService.findTransportEvents(eventType, bookingReference, equipmentReference),
                transportEquipmentEventService.findTransportEquipmentEvents(eventType, bookingReference, equipmentReference),
                equipmentEventService.findEquipmentEvents(eventType, bookingReference, equipmentReference)
        );
    }

    @Override
    public Mono<Event> save(Event event) {
        Mono<?> returnEvent;
        Flux<String> callbackUrls;

        switch (event.getEventType()) {
            case "SHIPMENT":
                returnEvent = shipmentEventService.save((ShipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), null);
                returnEvent.doOnNext(it->new CallbackHandler(callbackUrls, (ShipmentEvent) it).start()).subscribe();
                break;
            case "TRANSPORT":
                returnEvent = transportEventService.save((TransportEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), null);
                returnEvent.doOnNext(it->new CallbackHandler(callbackUrls, (TransportEvent) it).start()).subscribe();
                break;
            case "TRANSPORTEQUIPMENT":
                returnEvent = transportEquipmentEventService.save((TransportEquipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), ((TransportEquipmentEvent) event).getEquipmentReference());
                returnEvent.doOnNext(it->new CallbackHandler(callbackUrls, (TransportEquipmentEvent) it).start()).subscribe();
                break;
            case "EQUIPMENT":
                returnEvent = equipmentEventService.save((EquipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), ((EquipmentEvent) event).getEquipmentReference());
                returnEvent.doOnNext(it->new CallbackHandler(callbackUrls, (EquipmentEvent) it).start()).subscribe();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event.getEventType());
        }
        //Check all subscriptions
        return (Mono<Event>) returnEvent;
    }
}
