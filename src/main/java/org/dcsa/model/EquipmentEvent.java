package org.dcsa.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dcsa.model.enums.EmptyIndicatorCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.xml.bind.annotation.XmlRootElement;

@Table("equipment_event")
@Data
@XmlRootElement
@NoArgsConstructor
@Entity
//@Query= "SELECT a FROM EquipmentEvent a WHERE (:eventType IS NULL or a.eventType =:eventType) AND (?2 IS NULL or a.equipmentReference =?2) ")
//@NamedQueries({
//        @NamedQuery(name="EquipmentEvent.test",
//                query= "SELECT a FROM EquipmentEvent a WHERE (?1 IS NULL or a.eventType =?1) AND (?2 IS NULL or a.equipmentReference =?2) ")
//})
@JsonTypeName("EQUIPMENT")
public class EquipmentEvent extends Event implements GetId<String>{



    @JsonProperty("equipmentReference")
    @Column("equipment_reference")
    private String equipmentReference;

    @JsonProperty("emptyIndicatorCode")
    @Column("empty_indicator_code")
    private EmptyIndicatorCode emptyIndicatorCode;

    @JsonProperty("facilityTypeCode")
    @Column("facility_type_code")
    private String facilityTypeCode;

    @JsonProperty("UNLocationCode")
    @Column("un_location_code")
    private String UNLocationCode;

    @JsonProperty("facilityCode")
    @Column("facility_code")
    private String facilityCode;

    @JsonProperty("otherFacility")
    @Column("other_facility")
    private String otherFacility;


}
