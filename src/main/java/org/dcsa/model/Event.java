package org.dcsa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@Table("event")
@Data
@XmlRootElement
@NoArgsConstructor
@Entity
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EquipmentEvent.class, name="EQUIPMENT"),
        @JsonSubTypes.Type(value = TransportEvent.class, name="TRANSPORT"),
        @JsonSubTypes.Type(value = ShipmentEvent.class, name="SHIPMENT"),
        @JsonSubTypes.Type(value = TransportEquipmentEvent.class, name="TRANSPORTEQUIPMENT")
})
public class Event extends AuditBase implements GetId<String>{

    @Id
    @JsonProperty("eventID")
    @Column("event_id")
    private String id;

    @JsonProperty("eventDateTime")
    @Column("event_date_time")
    private Date eventDateTime;

    @JsonProperty("eventClassifierCode")
    @Column("event_classifier_code")
    private String eventClassifierCode;

    @JsonProperty("eventType")
    @Column("event_type")
    private String eventType;

    @JsonProperty("eventTypeCode")
    @Column("event_type_code")
    private String eventTypeCode;
}
