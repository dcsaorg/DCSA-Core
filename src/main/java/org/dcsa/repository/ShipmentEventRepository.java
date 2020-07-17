package org.dcsa.repository;

import org.dcsa.model.ShipmentEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ShipmentEventRepository extends ReactiveCrudRepository<ShipmentEvent, String> {

}
