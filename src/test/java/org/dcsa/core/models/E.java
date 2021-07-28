package org.dcsa.core.models;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("E_table")
public class E {
    private Long id;

    private String name;
}
