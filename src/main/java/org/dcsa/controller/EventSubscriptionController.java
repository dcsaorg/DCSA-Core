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
import org.dcsa.model.Event;
import org.dcsa.model.EventSubscription;
import org.dcsa.model.enums.EventType;
import org.dcsa.service.EventSubscriptionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "event-subscriptions", produces = {MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "Event Subscriptions", description = "The event subscription API")
public class EventSubscriptionController extends BaseController<EventSubscriptionService, EventSubscription, UUID> {

    private final EventSubscriptionService eventSubscriptionService;

    @Override
    EventSubscriptionService getService() {
        return eventSubscriptionService;
    }

    @Override
    String getType() {
        return "Events";
    }

    @Operation(summary = "Find all Events", description = "Finds all Events in the database", tags = { "Events" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventSubscription.class))))
    })
    @GetMapping
    public Flux<EventSubscription> findAll() {
        return eventSubscriptionService.findAll();
    }

    @Operation(summary = "Find all Events", description = "Finds all Events in the database", tags = { "Events" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = EventSubscription.class))))
    })
    @PostMapping( consumes = "application/json", produces = "application/json")
    public Mono<EventSubscription> save(@RequestBody EventSubscription body) {
        return eventSubscriptionService.save(body);
    }



}
