package dk.asseco.dcsa.service.impl;

import dk.asseco.dcsa.model.Event;
import dk.asseco.dcsa.repository.EventRepository;
import dk.asseco.dcsa.service.EventService;
import dk.asseco.dcsa.service.TransportEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Service
public class EventServiceImpl extends BaseServiceImpl<EventRepository, Event, String> implements EventService {
    private final EventRepository eventRepository;
    private final ShipmentEventServiceImpl shipmentEventService;
    private final TransportEventServiceImpl transportEventService;

    @Override
    EventRepository getRepository() {
        return eventRepository;
    }

    @Override
    public String getType() {
        return "Event";
    }


    public <T extends Event> Flux<T> findAllTypes() {
        return Flux.merge(
                (Flux<T>) shipmentEventService.findAll(),
                (Flux<T>) transportEventService.findAll()
        );
//        return Flux.merge(
//                shipmentEventService.findAll(),
//                transportEventService.findAll());
    }
}
