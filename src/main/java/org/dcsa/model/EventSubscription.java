package org.dcsa.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.UUID;

@Table("event_subscription")
@Data
@XmlRootElement
@NoArgsConstructor
@Entity
public class EventSubscription extends AuditBase implements GetId<UUID> {

    @Id
    @JsonProperty("subscriptionID")
    @Column("subscription_id")
    private UUID id;

    @JsonProperty("callbackUrl")
    @Column("callback_url")
    private String callbackUrl;

    @JsonIgnore
    @Column("event_type")
    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("bookingReference")
    @Column("booking_reference")
    private String bookingReference;

    @JsonProperty("billOfLadingNumber")
    @Column("bill_of_lading_number")
    private String billOfLadingNumber;

    @JsonProperty("equipmentReference")
    @Column("equipment_reference")
    private String equipmentReference;

}