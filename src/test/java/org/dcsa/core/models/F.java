package org.dcsa.core.models;

import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("F_table")
public class F {
    private Long id;
}
