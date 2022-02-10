package org.dcsa.core.models.combined;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.models.Address;
import org.dcsa.core.models.Order;

@Data
@EqualsAndHashCode(callSuper = true)
@JoinedWithModel(lhsFieldName = "warehouseAddressId", rhsModel = Address.class, rhsFieldName = "addressId", filterFields = {"address"})
@ToString(callSuper = true)
public class ExtendedOrder extends Order {

}
