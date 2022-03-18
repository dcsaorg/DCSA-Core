package org.dcsa.core.models;

import lombok.Data;
import org.dcsa.core.models.combined.CustomerStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("customer_table")
public class Customer {

    @Id
    @Column("customer_id")
    private Long id;

    @Column("customer_name")
    private String name;

    @Column("address_id")
    private Long addressId;

    @Column("customer_status")
    private CustomerStatus customerStatus;
}
