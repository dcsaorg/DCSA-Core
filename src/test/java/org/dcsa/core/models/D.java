package org.dcsa.core.models;

import lombok.Data;
import org.dcsa.core.model.ForeignKey;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("D_table")
public class D {

    @Id
    private Long id;

    @ForeignKey(into="c", foreignFieldName="id")
    private Long cId;

    @Transient
    private C c;
}
