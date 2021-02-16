package org.dcsa.core.models;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("city_table")
public class City {

    @Column("id")
    private String id;

    @Column("city_name")
    private String name;

    @Column("country_id")
    private Long countryId;
}
