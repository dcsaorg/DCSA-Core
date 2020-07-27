package org.dcsa.controller;

import org.dcsa.model.Event;
import org.dcsa.model.Events;
import org.dcsa.model.enums.EventType;
import org.dcsa.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "events", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "Events", description = "The event API")
public class EventController extends BaseController<EventService, Event, String> {

    private final EventService eventService;

    @Override
    EventService getService() {
        return eventService;
    }

    @Override
    String getType() {
        return "Events";
    }

    @Operation(summary = "Find all Events", description = "Finds all Events in the database", tags = { "Events" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Event.class))))
    })
    @GetMapping
    public Mono<Events> findAll(@RequestParam(required = false, defaultValue = "EQUIPMENT,SHIPMENT,TRANSPORT,TRANSPORTEQUIPMENT") List<EventType> eventType, @RequestParam(required = false) String bookingReference, @RequestParam(required = false) String equipmentReference) {

        return eventService.findAllWrapped(eventService.findAllTypes(eventType, bookingReference, equipmentReference));
    }

    @Operation(summary = "Find Event by ID", description = "Returns a single Event", tags = { "Event" }, parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description="Id of the Event to be obtained. Cannot be empty.", required=true),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @Override
    public Mono<Event> findById(@PathVariable String id) {
        return super.findById(id);
    }

    @Operation(summary = "Find all Events", description = "Finds all Events in the database", tags = { "Events" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation")
    })
    @PostMapping(consumes = "application/json", produces = "application/json")
    public Mono<Event> saveAll(@RequestBody Event event) {
        return eventService.saveAll(event);
    }

}
