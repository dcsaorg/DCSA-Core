package dk.asseco.dcsa.service.impl;

import dk.asseco.dcsa.model.EquipmentEvent;
import dk.asseco.dcsa.model.TransportEvent;
import dk.asseco.dcsa.repository.EquipmentEventRepository;
import dk.asseco.dcsa.repository.TransportEventRepository;
import dk.asseco.dcsa.service.EquipmentEventService;
import dk.asseco.dcsa.service.TransportEventService;
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
