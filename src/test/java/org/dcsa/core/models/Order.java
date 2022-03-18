package org.dcsa.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Table("order_table")
public class Order {

    @Id
    /* Deliberately different to text ExtendedOrder lookup */
    @Column("order_id")
    private Long id;

    @Column("orderline")
    private String orderline;

    // USE_DEFAULT_NAME seems redundant here but we use it to test that
    // we do not get an invalid "duplicate field name".
    @JsonProperty(JsonProperty.USE_DEFAULT_NAME)
    @Column("customer_id")
    private Long receiverId;

    @JsonProperty(JsonProperty.USE_DEFAULT_NAME)
    @Column("address_id")
    private Long warehouseAddressId;

    @Column("delivery_date")
    private OffsetDateTime deliveryDate;
}
