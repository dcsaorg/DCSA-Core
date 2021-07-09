package org.dcsa.core.models;

import lombok.Data;
import org.dcsa.core.model.ForeignKey;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.MapEntity;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("B_table")
@JoinedWithModel(lhsFieldName = "cId", rhsModel = C.class, rhsFieldName = "id")
@JoinedWithModel(lhsFieldName = "eId", lhsModel = C.class, rhsModel = E.class, rhsFieldName = "id", rhsJoinAlias = "e_alias", filterFields = {"name"})
@JoinedWithModel(lhsFieldName = "dId", rhsModel = D.class, rhsFieldName = "id")
public class B {
    private Long id;

    private Long cId;

    private Long dId;

    @MapEntity(joinAlias = "e_alias")
    private E e;

    @ForeignKey(into="f", foreignFieldName="id")
    private Long fId;

    private F f;
}
