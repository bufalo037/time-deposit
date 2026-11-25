package org.ikigaidigital.adapter.in.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.ikigaidigital.application.port.in.TimeDepositView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * A time deposit as returned by {@code GET /time-deposits}, with the README schema:
 * {@code id, planType, balance, days, withdrawals}.
 */
@Schema(name = "TimeDeposit", description = "A time deposit with its withdrawals")
record TimeDepositResponse(
        @Schema(description = "Time deposit id", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        int id,
        @Schema(description = "Plan type: basic, student or premium", example = "basic", requiredMode = Schema.RequiredMode.REQUIRED)
        String planType,
        @Schema(description = "Current balance, 2 decimals", example = "10008.33", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal balance,
        @Schema(description = "Days since the deposit was opened", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
        int days,
        @ArraySchema(arraySchema = @Schema(description = "Withdrawals of this deposit (empty if none)",
                requiredMode = Schema.RequiredMode.REQUIRED))
        List<WithdrawalResponse> withdrawals) {

    /** Money is a JSON number with 2 decimals (ADR 0007), so double artifacts never reach clients. */
    private static final int MONEY_SCALE = 2;

    static TimeDepositResponse from(TimeDepositView view) {
        return new TimeDepositResponse(
                view.id(),
                view.planType(),
                BigDecimal.valueOf(view.balance()).setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                view.days(),
                view.withdrawals().stream().map(WithdrawalResponse::from).toList());
    }
}
