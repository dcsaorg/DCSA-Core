package org.dcsa.repository;

import org.dcsa.model.EventSubscription;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EventSubscriptionRepository extends ReactiveCrudRepository<EventSubscription, String> {

    @Query("SELECT callback_url FROM \"dcsa_v1_1\".event_subscription a WHERE ((:eventType IS NULL or a.event_type =:eventType) AND (:equipmentReference IS NULL or a.equipment_reference =:equipmentReference)) OR a.event_type = ''")
    Flux<String> findSubscriptionsByFilters(@Param("eventType") String eventType, @Param("equipmentReference") String equipmentReference);
}
