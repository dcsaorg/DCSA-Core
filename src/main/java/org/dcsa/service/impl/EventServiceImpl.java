package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.exception.NotFoundException;
import org.dcsa.model.*;
import org.dcsa.model.enums.EventType;
import org.dcsa.repository.*;
import org.dcsa.service.EventService;
import org.dcsa.util.CallbackHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@Slf4j
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
    public <T extends Event> Mono<T> findAnyById(UUID id) {
        Mono<T> event;

        event = (Mono<T>) eventRepository.findById(UUID.randomUUID())
                .switchIfEmpty(transportEventService.findById(id))
                .switchIfEmpty(shipmentEventService.findById(id))
                .switchIfEmpty(transportEquipmentEventService.findById(id))
                .switchIfEmpty(equipmentEventService.findById(id))
                .switchIfEmpty(Mono.error(new NotFoundException("No event was found with id: " + id)));

        return event;
    }

    @Override
    public Mono<Events> findAllWrapped(Flux<Event> events) {
        Events eventsWrapped = new Events();
        eventsWrapped.setEvents(events.toStream().collect(Collectors.toList()));
        return Mono.just(eventsWrapped);
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
    public <T extends Event> Mono<T> saveAny(Event event) {
        Mono<T> returnEvent;
        Flux<String> callbackUrls;

        switch (event.getEventType()) {
            case "SHIPMENT":
                returnEvent = (Mono<T>) shipmentEventService.save((ShipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), null);
                log.warn("CALLBACK URLS: "+callbackUrls.blockFirst());
                returnEvent.doOnNext(it->new CallbackHandler(callbackUrls, (ShipmentEvent) it).start()).subscribe();
                break;
            case "TRANSPORT":
                returnEvent = (Mono<T>) transportEventService.save((TransportEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), null);
                returnEvent.doOnNext(it->new CallbackHandler(callbackUrls, (TransportEvent) it).start()).subscribe();
                break;
            case "TRANSPORTEQUIPMENT":
                returnEvent = (Mono<T>) transportEquipmentEventService.save((TransportEquipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), ((TransportEquipmentEvent) event).getEquipmentReference());
                returnEvent.doOnNext(it->new CallbackHandler(callbackUrls, (TransportEquipmentEvent) it).start()).subscribe();
                break;
            case "EQUIPMENT":
                returnEvent = (Mono<T>) equipmentEventService.save((EquipmentEvent) event);
                callbackUrls = eventSubscriptionRepository.findSubscriptionsByFilters(event.getEventType(), ((EquipmentEvent) event).getEquipmentReference());
                returnEvent.doOnNext(it->new CallbackHandler(callbackUrls, (EquipmentEvent) it).start()).subscribe();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event.getEventType());
        }
        //Check all subscriptions
        return returnEvent;
    }
}
