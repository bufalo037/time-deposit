package org.ikigaidigital.application.port.in;

import org.ikigaidigital.domain.Withdrawal;

import java.util.List;

/**
 * Read model returned by {@link ListTimeDepositsUseCase}: a time deposit together with its withdrawals.
 * Withdrawals aren't part of the shared {@code TimeDeposit} class (no breaking changes), so the two are combined
 * here (ADR 0003). {@code balance} keeps the domain's {@code double}; the web adapter decides its JSON format.
 */
public record TimeDepositView(int id, String planType, double balance, int days, List<Withdrawal> withdrawals) {

    public TimeDepositView {
        withdrawals = List.copyOf(withdrawals);
    }
}
