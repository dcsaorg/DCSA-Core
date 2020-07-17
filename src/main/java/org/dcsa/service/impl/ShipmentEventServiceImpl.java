package org.dcsa.service.impl;

import org.dcsa.model.Event;
import org.dcsa.model.ShipmentEvent;
import org.dcsa.repository.EventRepository;
import org.dcsa.repository.ShipmentEventRepository;
import org.dcsa.service.EventService;
import org.dcsa.service.ShipmentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Service
public class ShipmentEventServiceImpl extends BaseServiceImpl<ShipmentEventRepository, ShipmentEvent, String> implements ShipmentEventService {
    private final ShipmentEventRepository shipmentEventRepository;

    @Override
    ShipmentEventRepository getRepository() {
        return shipmentEventRepository;
    }

    @Override
    public String getType() {
        return "ShipmentEvent";
    }

}
