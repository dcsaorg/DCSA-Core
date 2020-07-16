package dk.asseco.dcsa.service.impl;

import dk.asseco.dcsa.model.TransportEquipmentEvent;
import dk.asseco.dcsa.repository.TransportEquipmentEventRepository;
import dk.asseco.dcsa.repository.TransportEventRepository;
import dk.asseco.dcsa.service.TransportEquipmentEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TransportEquipmentEventServiceImpl extends BaseServiceImpl<TransportEquipmentEventRepository, TransportEquipmentEvent, String> implements TransportEquipmentEventService {
    private final TransportEquipmentEventRepository transportEquipmentEventRepository;

    @Override
    TransportEquipmentEventRepository getRepository() {
        return transportEquipmentEventRepository;
    }

    @Override
    public String getType() {
        return "TransportEquipmentEvent";
    }

}
