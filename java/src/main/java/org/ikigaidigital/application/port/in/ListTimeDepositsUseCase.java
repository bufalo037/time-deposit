package org.ikigaidigital.application.port.in;

import java.util.List;

/** Incoming port: list all time deposits with their withdrawals (README: "retrieve all time deposits"). */
public interface ListTimeDepositsUseCase {

    /** All time deposits ordered by id, each with its withdrawals (an empty list if it has none). */
    List<TimeDepositView> listTimeDeposits();
}
