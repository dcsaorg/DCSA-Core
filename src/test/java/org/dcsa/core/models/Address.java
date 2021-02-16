package org.dcsa.core.models;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("address_table")
public class Address {
    @Column("address_id")
    private Long addressId;

    @Column("street_name")
    private String address;

    @Column("city_id")
    private Long cityId;
}