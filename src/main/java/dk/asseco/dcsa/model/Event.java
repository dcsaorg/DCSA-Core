package dk.asseco.dcsa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import dk.asseco.dcsa.model.enums.EventClassifierCode;
import dk.asseco.dcsa.model.enums.EventType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Date;

@Table("event")
@Data
@XmlRootElement
@NoArgsConstructor
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
