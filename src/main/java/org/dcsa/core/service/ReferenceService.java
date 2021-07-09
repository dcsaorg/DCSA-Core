package org.dcsa.core.service;

import org.dcsa.core.model.Reference;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ReferenceService extends ExtendedBaseService<Reference, UUID> {

    Flux<Reference> findByShippingInstructionID(String shippingInstructionID);
    Flux<Reference> findByShipmentID(UUID shipmentID);
    Flux<Reference> findByTransportDocumentReference(String transportDocumentReference);
}
