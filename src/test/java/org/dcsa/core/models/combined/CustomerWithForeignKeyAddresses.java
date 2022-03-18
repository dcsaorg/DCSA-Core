package org.dcsa.core.models.combined;

import lombok.Data;
import org.dcsa.core.model.ForeignKey;
import org.dcsa.core.models.Address;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("customer_table")
public class CustomerWithForeignKeyAddresses {

    @Id
    @Column("customer_id")
    private Long id;

    @Column("customer_name")
    private String name;

    @Column("delivery_address_id")
    @ForeignKey(into="deliveryAddress", foreignFieldName="addressId", viaJoinAlias = "delivery_address")
    private Long deliveryAddressId;

    @Transient
    private Address deliveryAddress;

    @Column("payment_address_id")
    private Long paymentAddressId;

    @Transient
    @ForeignKey(fromFieldName = "paymentAddressId", foreignFieldName="addressId", viaJoinAlias = "payment_address")
    private Address paymentAddress;
}
