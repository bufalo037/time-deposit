package org.ikigaidigital.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.ikigaidigital.domain.Withdrawal;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A withdrawal as returned by {@code GET /time-deposits}. It's nested under its time deposit, so the
 * {@code timeDepositId} is left out (ADR 0007).
 */
@Schema(name = "Withdrawal", description = "A withdrawal made from the time deposit")
record WithdrawalResponse(
        @Schema(description = "Withdrawal id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int id,
        @Schema(description = "Withdrawn amount, 2 decimals", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,
        @Schema(description = "Date of the withdrawal (ISO-8601)", example = "2026-09-15", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date) {

    static WithdrawalResponse from(Withdrawal withdrawal) {
        return new WithdrawalResponse(withdrawal.id(), withdrawal.amount(), withdrawal.date());
    }
}
