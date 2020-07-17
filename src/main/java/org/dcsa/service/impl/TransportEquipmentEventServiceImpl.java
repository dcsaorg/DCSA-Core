package org.dcsa.service.impl;

import org.dcsa.model.TransportEquipmentEvent;
import org.dcsa.repository.TransportEquipmentEventRepository;
import org.dcsa.repository.TransportEventRepository;
import org.dcsa.service.TransportEquipmentEventService;
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
