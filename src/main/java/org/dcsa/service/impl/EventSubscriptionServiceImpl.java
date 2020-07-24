package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.model.EventSubscription;
import org.dcsa.repository.EquipmentEventRepository;
import org.dcsa.repository.EventSubscriptionRepository;
import org.dcsa.service.EventSubscriptionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class EventSubscriptionServiceImpl extends BaseServiceImpl<EventSubscriptionRepository, EventSubscription, String> implements EventSubscriptionService {
    private final EventSubscriptionRepository eventSubscrpitionRepository;


    @Override
    EventSubscriptionRepository getRepository() {
        return eventSubscrpitionRepository;
    }

    @Override
    public String getType() {
        return "EquipmentEvent";
    }

    @Override
    public Mono<EventSubscription> save(EventSubscription eventSubscription) {
        return super.save(eventSubscription);
    }
}
