package org.ikigaidigital.adapter.in.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * API metadata for the generated OpenAPI spec (ADR 0006). The server URL is relative ("/"), so the spec is identical
 * on every port and machine. That's required for the committed {@code openapi.yaml} contract test.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "XA Bank Time Deposit API",
                version = "1.0.0",
                description = "List time deposits with their withdrawals and apply monthly interest to all balances."),
        servers = @Server(url = "/", description = "Current host"))
class OpenApiConfiguration {
}
