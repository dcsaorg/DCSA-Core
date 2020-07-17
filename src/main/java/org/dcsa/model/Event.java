package org.dcsa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Date;

@Table("event")
@Data
@XmlRootElement
@NoArgsConstructor
@Entity
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
