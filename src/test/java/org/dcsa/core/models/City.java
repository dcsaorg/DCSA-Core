package org.dcsa.core.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("city_table")
public class City {

    @Id
    @Column("id")
    private String id;

    @Column("city_name")
    private String name;

    @Column("country_id")
    private Long countryId;
}
