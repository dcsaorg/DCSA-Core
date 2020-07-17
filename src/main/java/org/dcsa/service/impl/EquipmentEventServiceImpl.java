package org.dcsa.service.impl;

import org.dcsa.model.EquipmentEvent;
import org.dcsa.model.TransportEvent;
import org.dcsa.repository.EquipmentEventRepository;
import org.dcsa.repository.TransportEventRepository;
import org.dcsa.service.EquipmentEventService;
import org.dcsa.service.TransportEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EquipmentEventServiceImpl extends BaseServiceImpl<EquipmentEventRepository, EquipmentEvent, String> implements EquipmentEventService {
    private final EquipmentEventRepository equipmentEventRepository;

    @Override
    EquipmentEventRepository getRepository() {
        return equipmentEventRepository;
    }

    @Override
    public String getType() {
        return "EquipmentEvent";
    }

}
