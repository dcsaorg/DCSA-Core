package org.dcsa.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dcsa.model.enums.EventType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Table("event_subscription")
@Data
@XmlRootElement
@NoArgsConstructor
@Entity
public class EventSubscription extends AuditBase implements GetId<String> {

    @Id
    @JsonProperty("subscriptionID")
    @Column("subscription_id")
    private String id;

    @JsonProperty("callbackUrl")
    @Column("callback_url")
    private String callbackUrl;

    @JsonIgnore
    @Column("event_type")
    @JsonProperty("eventType")
    private String eventType;

//    public List<EventType> getEventType() {
//        return Objects.requireNonNullElseGet(eventType, () -> convertCommaSeparatedStringToEnumList(eventTypes));
//    }


//    public String getEventTypes() {
//        eventTypes= eventType.stream()
//              .map(EventType::toString)
//              .collect(Collectors.joining(","));
//        return eventTypes;
//    }
//
//    public void setEventTypes(String eventTypes) {
//        this.eventTypes=eventTypes;
//        this.eventType=convertCommaSeparatedStringToEnumList(eventTypes);
//    }
//
//    public void setEventType(List<EventType> eventType) {
//        this.eventType=eventType;
//        this.eventTypes= eventType.stream()
//              .map(EventType::toString)
//              .collect(Collectors.joining(","));
//    }


//    @JsonProperty("eventType")
//    private List<EventType> eventType;

    @JsonProperty("bookingReference")
    @Column("booking_reference")
    private String bookingReference;

    @JsonProperty("billOfLadingNumber")
    @Column("bill_of_lading_number")
    private String billOfLadingNumber;

    @JsonProperty("equipmentReference")
    @Column("equipment_reference")
    private String equipmentReference;

    private List<EventType> convertCommaSeparatedStringToEnumList(String commaSeparatedList) {
        List<EventType> eventTypesEnumList = new ArrayList<>();
        if (commaSeparatedList != null && !commaSeparatedList.isEmpty()) {
            for (String type : commaSeparatedList.split(",")) {
                eventTypesEnumList.add(EventType.valueOf(type));
            }
        }
        return eventTypesEnumList;
    }
}
