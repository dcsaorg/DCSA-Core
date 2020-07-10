package dk.asseco.dcsa.repository;

import dk.asseco.dcsa.model.Event;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EventRepository extends ReactiveCrudRepository<Event, String> {

}
