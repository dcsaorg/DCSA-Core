package org.dcsa.repository;

import org.dcsa.model.TransportEquipmentEvent;
import org.dcsa.model.enums.EventType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface TransportEquipmentEventRepository extends ReactiveCrudRepository<TransportEquipmentEvent, UUID> {

    @Query("SELECT * FROM \"dcsa_v1_1\".transport_equipment_event a WHERE (:eventType IS NULL or a.event_type =:eventType) AND (:equipmentReference IS NULL or a.equipment_reference =:equipmentReference) ")
    Flux<TransportEquipmentEvent> findTransportEquipmentEventsByFilters(@Param("eventType") EventType eventType, String bookingReference, @Param("equipmentReference") String equipmentReference);
}
