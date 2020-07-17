package org.dcsa.repository;

import org.dcsa.model.TransportEquipmentEvent;
import org.dcsa.model.TransportEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TransportEquipmentEventRepository extends ReactiveCrudRepository<TransportEquipmentEvent, String> {

}
