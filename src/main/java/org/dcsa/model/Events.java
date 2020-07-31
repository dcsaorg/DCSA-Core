package org.dcsa.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@Data
@XmlRootElement
@NoArgsConstructor
public class Events {

    public Events(Event event){
        events = new ArrayList<>();
        events.add(event);
    }

    private List<Event> events;
}
