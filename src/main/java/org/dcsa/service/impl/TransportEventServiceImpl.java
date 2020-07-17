package org.dcsa.service.impl;

import org.dcsa.model.Event;
import org.dcsa.model.ShipmentEvent;
import org.dcsa.model.TransportEvent;
import org.dcsa.repository.EventRepository;
import org.dcsa.repository.TransportEventRepository;
import org.dcsa.service.TransportEventService;
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
