package org.ikigaidigital.adapter.in.web;

import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.TimeDepositView;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web slice: paths, methods, status codes and the exact JSON contract, with the use cases mocked. */
@WebMvcTest(TimeDepositController.class)
class TimeDepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListTimeDepositsUseCase listTimeDeposits;

    @MockitoBean
    private UpdateAllBalancesUseCase updateAllBalances;

    @Nested
    class GetAllTimeDeposits {

        @Test
        void returnsEveryDepositWithTheReadmeSchemaAndNestedWithdrawals() throws Exception {
            when(listTimeDeposits.listTimeDeposits()).thenReturn(List.of(
                    new TimeDepositView(1, "basic", 10000.0, 20, List.of()),
                    new TimeDepositView(2, "basic", 10008.33, 120, List.of(
                            new Withdrawal(1, 2, new BigDecimal("500.00"), LocalDate.of(2026, 9, 15))))));

            // strict = true: exactly these fields, no extras (e.g. no timeDepositId inside withdrawals)
            mockMvc.perform(get("/time-deposits"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("""
                            [
                              {"id": 1, "planType": "basic", "balance": 10000.00, "days": 20, "withdrawals": []},
                              {"id": 2, "planType": "basic", "balance": 10008.33, "days": 120,
                               "withdrawals": [{"id": 1, "amount": 500.00, "date": "2026-09-15"}]}
                            ]
                            """, org.springframework.test.json.JsonCompareMode.STRICT));
        }

        @Test
        void writesMoneyAsANumberWithTwoDecimalsWithoutDoubleArtifacts() throws Exception {
            when(listTimeDeposits.listTimeDeposits()).thenReturn(List.of(
                    new TimeDepositView(1, "basic", 1000.9300000000001, 60, List.of())));

            mockMvc.perform(get("/time-deposits"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("\"balance\":1000.93,")));
        }

        @Test
        void returnsAnEmptyArrayWhenThereAreNoDeposits() throws Exception {
            when(listTimeDeposits.listTimeDeposits()).thenReturn(List.of());

            mockMvc.perform(get("/time-deposits"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        @Test
        void neverUpdatesBalances() throws Exception {
            when(listTimeDeposits.listTimeDeposits()).thenReturn(List.of());

            mockMvc.perform(get("/time-deposits"));

            verify(updateAllBalances, never()).updateAllBalances();
        }
    }

    @Nested
    class UpdateAllBalances {

        @Test
        void updatesAllBalancesAndReturns204WithoutBody() throws Exception {
            mockMvc.perform(post("/time-deposits/balances/update"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(updateAllBalances).updateAllBalances();
        }

        @Test
        void rejectsOtherMethodsBecauseTheUpdateIsNotIdempotent() throws Exception {
            mockMvc.perform(put("/time-deposits/balances/update"))
                    .andExpect(status().isMethodNotAllowed());

            verify(updateAllBalances, never()).updateAllBalances();
        }
    }
}
