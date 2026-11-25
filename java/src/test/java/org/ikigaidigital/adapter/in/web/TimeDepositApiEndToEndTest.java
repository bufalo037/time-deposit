package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Both endpoints end to end: HTTP → controller → service → legacy calculator → persistence adapter → PostgreSQL
 * (Testcontainers, V1 schema + V2 seed). {@code @Transactional} rolls each test back, so the seed stays intact for
 * the next test. Expected balances match those pinned by {@code SeedDataTest} (captured from the original calculator).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
@Transactional
class TimeDepositApiEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsTheSeededDepositsWithTheirWithdrawals() throws Exception {
        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"id": 1, "planType": "basic",   "balance": 10000.00, "days": 20,  "withdrawals": []},
                          {"id": 2, "planType": "basic",   "balance": 10000.00, "days": 120,
                           "withdrawals": [{"id": 1, "amount": 500.00, "date": "2026-09-15"}]},
                          {"id": 3, "planType": "student", "balance": 5000.00,  "days": 200,
                           "withdrawals": [{"id": 2, "amount": 250.00, "date": "2026-07-01"}]},
                          {"id": 4, "planType": "student", "balance": 5000.00,  "days": 400, "withdrawals": []},
                          {"id": 5, "planType": "premium", "balance": 20000.00, "days": 40,  "withdrawals": []},
                          {"id": 6, "planType": "premium", "balance": 20000.00, "days": 90,
                           "withdrawals": [{"id": 3, "amount": 1000.00, "date": "2026-09-10"},
                                           {"id": 4, "amount": 500.00,  "date": "2026-10-20"}]}
                        ]
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void updateAppliesOneMonthOfInterestAndTheListShowsTheNewBalances() throws Exception {
        mockMvc.perform(post("/time-deposits/balances/update"))
                .andExpect(status().isNoContent());

        // ids 2, 3, 6 are past their interest start; 1 (grace period), 4 (over a year), 5 (before day 46) are not
        mockMvc.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"id": 1, "balance": 10000.00, "days": 20},
                          {"id": 2, "balance": 10008.33, "days": 120},
                          {"id": 3, "balance": 5012.50,  "days": 200},
                          {"id": 4, "balance": 5000.00,  "days": 400},
                          {"id": 5, "balance": 20000.00, "days": 40},
                          {"id": 6, "balance": 20083.33, "days": 90}
                        ]
                        """, JsonCompareMode.LENIENT));
    }
}
