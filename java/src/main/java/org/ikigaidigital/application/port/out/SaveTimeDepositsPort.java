package org.ikigaidigital.application.port.out;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/** Outgoing port: persist the balances of existing time deposits. */
public interface SaveTimeDepositsPort {

    /**
     * Stores the current balance of each given (already persisted) time deposit. Plan type and days aren't changed:
     * {@code updateBalance} only changes the balance.
     */
    void saveAll(List<TimeDeposit> timeDeposits);
}
