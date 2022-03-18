package org.dcsa.core.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("C_table")
public class C {
    @Id
    private Long id;

    private Long eId;
}
