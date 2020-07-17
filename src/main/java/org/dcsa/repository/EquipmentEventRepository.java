package org.dcsa.repository;

import org.dcsa.model.EquipmentEvent;
import org.dcsa.model.TransportEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EquipmentEventRepository extends ReactiveCrudRepository<EquipmentEvent, String> {

}
