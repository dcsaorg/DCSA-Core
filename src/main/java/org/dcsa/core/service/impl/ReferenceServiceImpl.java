package org.dcsa.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.model.Reference;
import org.dcsa.core.repository.ReferenceRepository;
import org.dcsa.core.service.ReferenceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ReferenceServiceImpl extends ExtendedBaseServiceImpl<ReferenceRepository, Reference, UUID> implements ReferenceService {
    private final ReferenceRepository referenceRepository;

    @Override
    public ReferenceRepository getRepository() {
        return referenceRepository;
    }

    @Override
    public Flux<Reference> findByShippingInstructionID(String shippingInstructionID) {
        return referenceRepository.findByShippingInstructionID(shippingInstructionID);
    }

    @Override
    public Flux<Reference> findByShipmentID(UUID shipmentID) {
        return referenceRepository.findByShipmentID(shipmentID);
    }

    @Override
    public Flux<Reference> findByTransportDocumentReference(String transportDocumentReference) {
        return referenceRepository.findByTransportDocumentReference(transportDocumentReference);
    }
}