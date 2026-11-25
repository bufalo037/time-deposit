package org.ikigaidigital.application.port.out;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/** Outgoing port: read all time deposits from storage. */
public interface LoadTimeDepositsPort {

    /** All time deposits, ordered by id. */
    List<TimeDeposit> loadAllTimeDeposits();
}
