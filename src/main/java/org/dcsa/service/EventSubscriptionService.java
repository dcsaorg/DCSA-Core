package org.dcsa.service;

import org.dcsa.model.Event;
import org.dcsa.model.EventSubscription;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface EventSubscriptionService extends BaseService<EventSubscription, UUID>{

    Flux<EventSubscription> findAll();

    Mono<EventSubscription> findById(UUID id);

    Mono<EventSubscription> save(EventSubscription eventSubscription);

}
