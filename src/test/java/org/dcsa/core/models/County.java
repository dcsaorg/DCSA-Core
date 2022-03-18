package org.dcsa.core.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("country_table")
public class County {

    @Id
    @Column("id")
    private String id;

    @Column("country_name")
    private String countryName;
}
