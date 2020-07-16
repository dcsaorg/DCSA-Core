package dk.asseco.dcsa.repository;

import dk.asseco.dcsa.model.EquipmentEvent;
import dk.asseco.dcsa.model.TransportEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EquipmentEventRepository extends ReactiveCrudRepository<EquipmentEvent, String> {

}
