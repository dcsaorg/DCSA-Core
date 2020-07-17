package org.dcsa.repository;

import org.dcsa.model.TransportEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TransportEventRepository extends ReactiveCrudRepository<TransportEvent, String> {

}
