package org.dcsa.core.models.combined;

import com.fasterxml.jackson.annotation.JsonProperty;;
import lombok.Data;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.ModelClass;
import org.dcsa.core.model.PrimaryModel;
import org.dcsa.core.model.ViaJoinAlias;
import org.dcsa.core.models.*;

@Data
@PrimaryModel(Order.class)
@JoinedWithModel(lhsFieldName = "receiverId", rhsModel = Customer.class, rhsFieldName = "addressId")
@JoinedWithModel(lhsModel = Customer.class, lhsFieldName = "addressId", rhsModel = Address.class, rhsJoinAlias = "customer_address", rhsFieldName = "addressId")
@JoinedWithModel(lhsFieldName = "warehouseAddressId", rhsModel = Address.class, rhsJoinAlias = "warehouse_address", rhsFieldName = "addressId")

/* Joins only present for filtering */
@JoinedWithModel(lhsJoinAlias = "customer_address", lhsFieldName = "cityId", rhsModel = City.class, rhsFieldName = "id")
@JoinedWithModel(lhsModel = City.class, lhsFieldName = "countryId", rhsModel = County.class, rhsFieldName = "id", filterFields = {"countryName"})
public class OrderWithEverything extends Order {

    @ModelClass(value = Customer.class, fieldName = "name")
    private String customerName;

    @ModelClass(value = Address.class, fieldName = "address")
    @ViaJoinAlias("customer_address")
    private String customerAddress;

    @ModelClass(value = Address.class, fieldName = "address")
    @ViaJoinAlias("warehouse_address")
    @JsonProperty("warehouse")
    private String warehouseAddress;
}
