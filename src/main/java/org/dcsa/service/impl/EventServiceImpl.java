package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.base.service.impl.ExtendedBaseServiceImpl;
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
public class EventServiceImpl extends ExtendedBaseServiceImpl<EventRepository, Event, UUID> implements EventService {
    private final ShipmentEventServiceImpl shipmentEventService;
    private final TransportEventServiceImpl transportEventService;
    private final TransportEquipmentEventServiceImpl transportEquipmentEventService;
    private final EquipmentEventServiceImpl equipmentEventService;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final EventRepository eventRepository;


    @Override
    public EventRepository getRepository() {
        return eventRepository;
    }

    @Override
    public Class<Event> getModelClass() {
        return Event.class;
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
        switch (event.getEventType()) {
            case SHIPMENT:
                return shipmentEventService.save((ShipmentEvent) event).doOnNext(
                        e -> new CallbackHandler(
                                eventSubscriptionRepository.findSubscriptionsByFilters(e.getEventType(),
                                        null), e)
                                .start()
                ).map(e -> e);
            case TRANSPORT:
                return transportEventService.save((TransportEvent) event).doOnNext(
                        e -> new CallbackHandler(
                                eventSubscriptionRepository.findSubscriptionsByFilters(e.getEventType(),
                                        null), e)
                                .start()
                ).map(e -> e);
            case TRANSPORTEQUIPMENT:
                return transportEquipmentEventService.save((TransportEquipmentEvent) event).doOnNext(
                        e -> new CallbackHandler(
                                eventSubscriptionRepository.findSubscriptionsByFilters(e.getEventType(),
                                        e.getEquipmentReference()), e)
                                .start()
                ).map(e -> e);
            case EQUIPMENT:
                return equipmentEventService.save((EquipmentEvent) event).doOnNext(
                        e -> new CallbackHandler(
                                eventSubscriptionRepository.findSubscriptionsByFilters(e.getEventType(),
                                e.getEquipmentReference()), e)
                                .start()
                ).map(e -> e);
            default:
                return Mono.error(new IllegalStateException("Unexpected value: " + event.getEventType()));
        }
    }
}
