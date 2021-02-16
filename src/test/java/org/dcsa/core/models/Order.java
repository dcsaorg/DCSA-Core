package org.dcsa.core.models;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("order_table")
public class Order {

    /* Deliberately different to text ExtendedOrder lookup */
    @Column("order_id")
    private Long id;

    @Column("orderline")
    private String orderline;

    @Column("customer_id")
    private Long receiverId;

    @Column("address_id")
    private Long warehouseAddressId;
}
