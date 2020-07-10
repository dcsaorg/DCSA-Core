package dk.asseco.dcsa.service.impl;

import dk.asseco.dcsa.model.Event;
import dk.asseco.dcsa.model.ShipmentEvent;
import dk.asseco.dcsa.repository.EventRepository;
import dk.asseco.dcsa.repository.ShipmentEventRepository;
import dk.asseco.dcsa.service.EventService;
import dk.asseco.dcsa.service.ShipmentEventService;
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
