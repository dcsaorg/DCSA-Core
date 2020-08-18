package org.dcsa.repository;

import org.dcsa.model.Event;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface EventRepository extends R2dbcRepository<Event, UUID>, ExtendedRepository<Event> {

}
