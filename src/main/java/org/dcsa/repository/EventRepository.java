package org.dcsa.repository;

import org.dcsa.model.Event;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface EventRepository extends ReactiveCrudRepository<Event, UUID> {

}
