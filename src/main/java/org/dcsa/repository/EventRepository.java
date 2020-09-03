package org.dcsa.repository;

import org.dcsa.base.repository.ExtendedRepository;
import org.dcsa.model.Event;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface EventRepository extends ExtendedRepository<Event, UUID> {

}
