package org.dcsa.core.models;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("customer_table")
public class Customer {

    @Column("customer_id")
    private Long id;

    @Column("customer_name")
    private String name;

    @Column("address_id")
    private Long addressId;
}
