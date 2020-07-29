package org.dcsa.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.xml.bind.annotation.XmlRootElement;

@Table("shipment_event")
@Data
@XmlRootElement
@NoArgsConstructor
@JsonTypeName("SHIPMENT")
public class ShipmentEvent extends Event{

    @JsonProperty("shipmentInformationTypeCode")
    @Column("shipment_information_type_code")
    private String shipmentInformationTypeCode;

}
