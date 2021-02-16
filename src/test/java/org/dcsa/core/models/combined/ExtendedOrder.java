package org.dcsa.core.models.combined;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.ModelClass;
import org.dcsa.core.model.PrimaryModel;
import org.dcsa.core.models.Address;
import org.dcsa.core.models.Order;

@Data
@PrimaryModel(Order.class)
@JoinedWithModel(lhsFieldName = "warehouseAddressId", rhsModel = Address.class, rhsFieldName = "addressId")
public class ExtendedOrder extends Order {

    @ModelClass(value = Address.class, fieldName = "address")
    @JsonProperty("warehouse")
    private String warehouseAddress;
}
