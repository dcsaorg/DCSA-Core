package org.dcsa.service;

import org.dcsa.model.Event;
import org.dcsa.model.EventSubscription;
import org.dcsa.model.enums.EventType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EventSubscriptionService extends BaseService<EventSubscription, String>{

    Flux<EventSubscription> findAll();

    Mono<EventSubscription> findById(String id);

    Mono<EventSubscription> save(EventSubscription eventSubscription);

}
