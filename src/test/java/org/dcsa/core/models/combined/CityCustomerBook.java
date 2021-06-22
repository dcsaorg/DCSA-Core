package org.dcsa.core.models.combined;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dcsa.core.model.ForeignKey;
import org.dcsa.core.models.City;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;

@Data
@EqualsAndHashCode(callSuper = true)
public class CityCustomerBook extends CustomerBook {
    @Column("city_id")
    @ForeignKey(into="city", foreignFieldName="id")
    private String cityId;

    @Transient
    private City city;
}
