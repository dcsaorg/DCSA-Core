package org.dcsa.repository;

import org.dcsa.model.Event;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EventRepository extends ReactiveCrudRepository<Event, String> {

}
