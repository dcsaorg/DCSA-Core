package org.dcsa.core.controller.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ApiVersionHeaderFilter implements WebFilter {

  private final String apiVersion;

  public ApiVersionHeaderFilter(@Value("${dcsa.specification.version:N/A}") String apiVersion) {
    this.apiVersion = apiVersion;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    exchange.getResponse().getHeaders().add("API-Version", apiVersion);
    return chain.filter(exchange);
  }
}
