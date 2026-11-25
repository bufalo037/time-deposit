package org.ikigaidigital.application.port.in;

/** Incoming port: update the balances of all time deposits (README: "update the balances of all time deposits"). */
public interface UpdateAllBalancesUseCase {

    /**
     * Applies one month of interest to every time deposit using the existing
     * {@code TimeDepositCalculator.updateBalance}, then stores the new balances. Not idempotent: each call adds
     * another month of interest. Days aren't advanced, because {@code updateBalance} never changes them and the
     * README gives no rule for it.
     */
    void updateAllBalances();
}
