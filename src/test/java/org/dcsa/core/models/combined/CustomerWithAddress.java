package org.dcsa.core.models.combined;

import lombok.Data;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.MapEntity;
import org.dcsa.core.model.ModelClass;
import org.dcsa.core.model.PrimaryModel;
import org.dcsa.core.models.Address;
import org.dcsa.core.models.Customer;

@Data
@PrimaryModel(Customer.class)
@JoinedWithModel(lhsFieldName = "addressId", rhsModel = Address.class, rhsFieldName = "addressId")
public class CustomerWithAddress {

    private Long id;

    private String name;

    private Long addressId;

    @MapEntity
    private Address address;
}
