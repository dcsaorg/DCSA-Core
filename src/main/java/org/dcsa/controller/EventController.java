package org.dcsa.controller;

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
import org.dcsa.exception.GetException;
import org.dcsa.model.Event;
import org.dcsa.model.Events;
import org.dcsa.service.EventService;
import org.dcsa.util.ExtendedParameters;
import org.dcsa.util.ExtendedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "events", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "Events", description = "The event API")
public class EventController extends BaseController<EventService, Event, UUID> {

    private final EventService eventService;

    @Autowired
    private ExtendedParameters extendedParameters;

    @Override
    String getType() {
        return getService().getModelClass().getSimpleName();
    }

    @Override
    EventService getService() {
        return eventService;
    }

    @Operation(summary = "Find all Events", description = "Finds all Events in the database", tags = { "Events" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Event.class))))
    })
    @GetMapping
    public Mono<Events> findAll(ServerHttpResponse response, ServerHttpRequest request) {
        ExtendedRequest<Event> extendedRequest = new ExtendedRequest<>(extendedParameters, Event.class);
        try {
            Map<String,String> params = request.getQueryParams().toSingleValueMap();;
            extendedRequest.parseParameter(params);
        } catch (GetException getException) {
            return Mono.error(getException);
        }

        Flux<Event> result = getService().findAllExtended(extendedRequest);
        // Add Link headers to the response
        extendedRequest.insertPaginationHeaders(response, request);
        return eventService.findAllWrapped(result);
    }

    @Operation(summary = "Find Event by ID", description = "Returns a single Event", tags = { "Event" }, parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description="Id of the Event to be obtained. Cannot be empty.", required=true),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
//    @GetMapping("{id}")
    @Override
    public Mono<Event> findById(@PathVariable UUID id) {
        return super.findById(id);
    }

    @Operation(summary = "Save any type of event", description = "Saves any type of event", tags = { "Events" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation")
    })
    @PostMapping(consumes = "application/json", produces = "application/json")
    @Override
    public Mono<Event> save(@RequestBody Event event) {
        return super.save(event);
    }

}
