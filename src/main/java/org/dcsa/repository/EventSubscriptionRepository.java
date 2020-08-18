package org.dcsa.repository;

import org.dcsa.model.EventSubscription;
import org.dcsa.model.enums.EventType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface EventSubscriptionRepository extends R2dbcRepository<EventSubscription, UUID>, ExtendedRepository<EventSubscription> {

    @Query("SELECT callback_url FROM \"dcsa_v1_1\".event_subscription a WHERE ((:eventType IS NULL or a.event_type =:eventType) AND (:equipmentReference IS NULL or a.equipment_reference =:equipmentReference)) OR a.event_type = ''")
    Flux<String> findSubscriptionsByFilters(@Param("eventType") EventType eventType, @Param("equipmentReference") String equipmentReference);
}
