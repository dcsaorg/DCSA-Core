package org.dcsa.util;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.model.*;
import reactor.core.publisher.Flux;

import java.net.ConnectException;

import static io.restassured.RestAssured.given;

@Slf4j
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
        callbackUrls.toStream().forEach(callbackUrl ->{
            try{
                given()
                        .contentType("application/json")
                        .body(event)
                        .post(callbackUrl);
            }
        catch (Exception e)
        {
            log.warn("Failed to connect to "+callbackUrl+" with error: "+e.getMessage());
            return;
        }
        });
    }
}
