package dk.asseco.dcsa.controller;

import dk.asseco.dcsa.model.Event;
import dk.asseco.dcsa.service.EventService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "events", consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "Events", description = "the event API")
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
            @ApiResponse(responseCode = "200", description = "successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Event.class))))
    })
    @Override
    public Flux<Event> findAll() {
        return eventService.findAllTypes();
    }

    @Operation(summary = "Find Event by ID", description = "Returns a single Event", tags = { "Event" }, parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description="Id of the Event to be obtained. Cannot be empty.", required=true),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @Override
    public Mono<Event> findById(@PathVariable String id) {
        return super.findById(id);
    }
}
