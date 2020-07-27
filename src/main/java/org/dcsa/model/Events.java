package org.dcsa.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement
@NoArgsConstructor
public class Events {

    private List<Event> events;
}
