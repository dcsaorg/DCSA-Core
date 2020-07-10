package dk.asseco.dcsa.repository;

import dk.asseco.dcsa.model.Event;
import dk.asseco.dcsa.model.TransportEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TransportEventRepository extends ReactiveCrudRepository<TransportEvent, String> {

}
