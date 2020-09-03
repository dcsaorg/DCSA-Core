package org.dcsa.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.base.service.impl.ExtendedBaseServiceImpl;
import org.dcsa.model.EventSubscription;
import org.dcsa.repository.EventSubscriptionRepository;
import org.dcsa.service.EventSubscriptionService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class EventSubscriptionServiceImpl extends ExtendedBaseServiceImpl<EventSubscriptionRepository, EventSubscription, UUID> implements EventSubscriptionService {
    private final EventSubscriptionRepository eventSubscriptionRepository;

    @Override
    public EventSubscriptionRepository getRepository() {
        return eventSubscriptionRepository;
    }

    @Override
    public Class<EventSubscription> getModelClass() {
        return EventSubscription.class;
    }
}
