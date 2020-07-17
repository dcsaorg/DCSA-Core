package org.dcsa.repository;

import org.dcsa.model.TransportEvent;
import org.dcsa.model.enums.EventType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface TransportEventRepository extends ReactiveCrudRepository<TransportEvent, String> {

    @Query("SELECT * FROM \"dcsa_v1_1\".transport_event a WHERE :eventType IS NULL or a.event_type =:eventType ")
    Flux<TransportEvent> findTransportEventsByFilters(@Param("eventType") EventType eventType);
}
