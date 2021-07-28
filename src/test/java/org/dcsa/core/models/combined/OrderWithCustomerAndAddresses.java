package org.dcsa.core.models.combined;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.MapEntity;
import org.dcsa.core.models.Address;
import org.dcsa.core.models.Customer;
import org.dcsa.core.models.Order;

@Data
@EqualsAndHashCode(callSuper = true)
@JoinedWithModel(lhsFieldName = "receiverId", rhsModel = Customer.class, rhsFieldName = "addressId")
@JoinedWithModel(lhsModel = Customer.class, lhsFieldName = "addressId", rhsModel = Address.class, rhsJoinAlias = "customer_address", rhsFieldName = "addressId")
@JoinedWithModel(lhsFieldName = "warehouseAddressId", rhsModel = Address.class, rhsJoinAlias = "warehouse_address", rhsFieldName = "addressId")
public class OrderWithCustomerAndAddresses extends Order {

    @MapEntity
    private Customer customer;

    @MapEntity(joinAlias = "customer_address")
    private Address customerAddress;

    @MapEntity(joinAlias = "warehouse_address")
    @JsonProperty("warehouse")
    private Address warehouseAddress;
}
