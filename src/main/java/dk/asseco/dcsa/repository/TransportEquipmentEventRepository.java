package dk.asseco.dcsa.repository;

import dk.asseco.dcsa.model.TransportEquipmentEvent;
import dk.asseco.dcsa.model.TransportEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TransportEquipmentEventRepository extends ReactiveCrudRepository<TransportEquipmentEvent, String> {

}
