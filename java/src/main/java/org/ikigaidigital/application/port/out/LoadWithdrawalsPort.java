package org.ikigaidigital.application.port.out;

import org.ikigaidigital.domain.Withdrawal;

import java.util.List;

/** Outgoing port: read withdrawals from storage. */
public interface LoadWithdrawalsPort {

    /** All withdrawals, ordered by time deposit id, then date, then id. Loaded in one query (no N+1). */
    List<Withdrawal> loadAllWithdrawals();
}
