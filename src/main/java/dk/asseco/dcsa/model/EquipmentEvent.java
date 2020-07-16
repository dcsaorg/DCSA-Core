package dk.asseco.dcsa.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import dk.asseco.dcsa.model.enums.EmptyIndicatorCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.xml.bind.annotation.XmlRootElement;

@Table("transport_equipment_event")
@Data
@XmlRootElement
@NoArgsConstructor
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

    @JsonProperty("modeOfTransportCode")
    @Column("mode_of_transport_code")
    private String modeOfTransportCode;


}
