package dk.asseco.dcsa.service.impl;

import dk.asseco.dcsa.model.Event;
import dk.asseco.dcsa.model.ShipmentEvent;
import dk.asseco.dcsa.model.TransportEvent;
import dk.asseco.dcsa.repository.EventRepository;
import dk.asseco.dcsa.repository.TransportEventRepository;
import dk.asseco.dcsa.service.TransportEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Service
public class TransportEventServiceImpl extends BaseServiceImpl<TransportEventRepository, TransportEvent, String> implements TransportEventService {
    private final TransportEventRepository transportEventRepository;

    @Override
    TransportEventRepository getRepository() {
        return transportEventRepository;
    }

    @Override
    public String getType() {
        return "TransportEvent";
    }

}
