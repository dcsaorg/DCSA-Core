package dk.asseco.dcsa.repository;

import dk.asseco.dcsa.model.ShipmentEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ShipmentEventRepository extends ReactiveCrudRepository<ShipmentEvent, String> {

}
