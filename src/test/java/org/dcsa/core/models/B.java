package org.dcsa.core.models;

import lombok.Data;
import org.dcsa.core.model.ForeignKey;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.MapEntity;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("B_table")
@JoinedWithModel(lhsFieldName = "cId", rhsModel = C.class, rhsFieldName = "id", rhsJoinAlias = "c1")
@JoinedWithModel(lhsJoinAlias = "c1", lhsFieldName = "eId", lhsModel = C.class, rhsModel = E.class, rhsFieldName = "id", rhsJoinAlias = "e1")
@JoinedWithModel(lhsFieldName = "cId", rhsModel = C.class, rhsFieldName = "id", rhsJoinAlias = "c2")
@JoinedWithModel(lhsJoinAlias = "c2", lhsFieldName = "eId", lhsModel = C.class, rhsModel = E.class, rhsFieldName = "id", rhsJoinAlias = "e2")
@JoinedWithModel(lhsFieldName = "dId", rhsModel = D.class, rhsFieldName = "id")
public class B {
    private Long id;

    private Long cId;

    private Long dId;

    @MapEntity(joinAlias = "e1")
    @Transient
    private E e1;

    @MapEntity(joinAlias = "e2")
    @Transient
    private E e2;

    @ForeignKey(into="f", foreignFieldName="id")
    @Column("fId_column")
    private Long fId;

    @Transient
    private F f;

    @MapEntity
    @Transient
    private D d;
}
