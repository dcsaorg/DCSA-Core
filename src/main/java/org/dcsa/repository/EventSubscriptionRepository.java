package org.dcsa.repository;

import org.dcsa.model.EventSubscription;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EventSubscriptionRepository extends ReactiveCrudRepository<EventSubscription, String> {


}
