package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Code-first OpenAPI contract (ADR 0006). The spec springdoc generates from the controllers must equal the committed
 * {@code java/openapi.yaml}, so the contract can't drift silently. After an intentional API change, regenerate it with
 * {@code ./mvnw verify -Dopenapi.update=true} and commit it together with the change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
class OpenApiContractTest {

    /** Maven runs tests with java/ as the working directory. */
    private static final Path CONTRACT = Path.of("openapi.yaml");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void committedContractMatchesTheGeneratedSpec() throws Exception {
        String generated = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        if (Boolean.getBoolean("openapi.update")) {
            Files.writeString(CONTRACT, generated, StandardCharsets.UTF_8);
        }

        assertThat(CONTRACT)
                .as("java/openapi.yaml is out of date: run ./mvnw verify -Dopenapi.update=true and commit it")
                .exists();
        assertThat(Files.readString(CONTRACT, StandardCharsets.UTF_8)).isEqualTo(generated);
    }

    @Test
    void exposesExactlyTheTwoEndpointsTheReadmeAllows() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths.keys()", containsInAnyOrder(
                        "/time-deposits", "/time-deposits/balances/update")))
                .andExpect(jsonPath("$.paths['/time-deposits'].keys()", containsInAnyOrder("get")))
                .andExpect(jsonPath("$.paths['/time-deposits/balances/update'].keys()", containsInAnyOrder("post")));
    }
}
