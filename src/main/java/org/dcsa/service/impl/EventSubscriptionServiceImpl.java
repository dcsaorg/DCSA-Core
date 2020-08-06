package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.model.EventSubscription;
import org.dcsa.repository.EquipmentEventRepository;
import org.dcsa.repository.EventSubscriptionRepository;
import org.dcsa.service.EventSubscriptionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class EventSubscriptionServiceImpl extends BaseServiceImpl<EventSubscriptionRepository, EventSubscription, UUID> implements EventSubscriptionService {
    private final EventSubscriptionRepository eventSubscrpitionRepository;

    @Override
    EventSubscriptionRepository getRepository() {
        return eventSubscrpitionRepository;
    }

    @Override
    public String getType() {
        return "EquipmentEvent";
    }
}
