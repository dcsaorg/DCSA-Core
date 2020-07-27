package org.dcsa.util;

import org.dcsa.model.*;
import reactor.core.publisher.Flux;

import static io.restassured.RestAssured.given;

public class CallbackHandler extends Thread {

    Flux<String> callbackUrls;
    Event event;

    public CallbackHandler (Flux<String> callbackUrls, ShipmentEvent event){
        this.callbackUrls=callbackUrls;
        this.event=event;
    }

    public CallbackHandler (Flux<String> callbackUrls, TransportEvent event){
        this.callbackUrls=callbackUrls;
        this.event=event;
    }

    public CallbackHandler (Flux<String> callbackUrls, EquipmentEvent event){
        this.callbackUrls=callbackUrls;
        this.event=event;
    }
    public CallbackHandler (Flux<String> callbackUrls, TransportEquipmentEvent event){
        this.callbackUrls=callbackUrls;
        this.event=event;
    }

@Override
    public void run (){
        callbackUrls.toStream().forEach(callbackUrl ->
                given()
                        .contentType("application/json")
                        .body(event)
                        .when()
                        .post(callbackUrl).
                        then().
                        assertThat().
                        statusCode(200));
    }
}
