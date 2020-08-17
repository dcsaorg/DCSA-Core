package org.dcsa.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dcsa.model.enums.EventClassifierCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Table("aggregated_events")
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
public class Event extends AuditBase implements GetId<UUID> {

    @Id
    @JsonProperty("eventID")
    @Column("event_id")
    private UUID id;


    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public LocalDate getEventDateTime() {
        return eventDateTime;
    }

    @JsonProperty("eventDateTime")
    @Column("event_date_time")
    private LocalDate eventDateTime;

    @JsonProperty("eventClassifierCode")
    @Column("event_classifier_code")
    private EventClassifierCode eventClassifierCode;

    @JsonProperty("eventType")
    @Column("event_type")
    private String eventType;

    @JsonProperty("eventTypeCode")
    @Column("event_type_code")
    private String eventTypeCode;

    public void setEventClassifierCode(String eventClassifierCode) {
        this.eventClassifierCode = EventClassifierCode.valueOf(eventClassifierCode);
    }

    public void setEventClassifierCode(EventClassifierCode eventClassifierCode) {
        this.eventClassifierCode = eventClassifierCode;
    }
}
