package org.dcsa.core.models;

import lombok.Data;
import org.dcsa.core.model.ForeignKey;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("A_table")
public class A {
    private Long id;

    @ForeignKey(into="b", foreignFieldName="id")
    private Long bId;

    private B b;
}
