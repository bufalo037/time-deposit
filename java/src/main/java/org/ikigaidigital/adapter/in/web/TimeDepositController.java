package org.ikigaidigital.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The README's two endpoints, and no others (ADR 0007). Depends only on incoming ports (ADR 0003).
 * Invalid input and exception handling are out of scope (README).
 */
@RestController
@RequestMapping("/time-deposits")
@Tag(name = "Time deposits", description = "List time deposits and apply monthly interest")
class TimeDepositController {

    private final ListTimeDepositsUseCase listTimeDeposits;
    private final UpdateAllBalancesUseCase updateAllBalances;

    TimeDepositController(ListTimeDepositsUseCase listTimeDeposits, UpdateAllBalancesUseCase updateAllBalances) {
        this.listTimeDeposits = listTimeDeposits;
        this.updateAllBalances = updateAllBalances;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve all time deposits",
            description = "Returns every time deposit ordered by id, each with its withdrawals.")
    @ApiResponse(responseCode = "200", description = "All time deposits")
    List<TimeDepositResponse> getAllTimeDeposits() {
        return listTimeDeposits.listTimeDeposits().stream()
                .map(TimeDepositResponse::from)
                .toList();
    }

    @PostMapping("/balances/update")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update the balances of all time deposits",
            description = """
                    Applies one month of interest to every time deposit and stores the new balances \
                    (basic 1%, student 3% up to 1 year, premium 5% after 45 days, a year each, applied monthly; \
                    no interest for the first 30 days). \
                    Not idempotent: every call adds another month of interest. Use GET /time-deposits to see the result.""")
    @ApiResponse(responseCode = "204", description = "Balances updated")
    void updateAllBalances() {
        updateAllBalances.updateAllBalances();
    }
}
