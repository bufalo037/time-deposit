package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.TimeDepositView;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.application.port.out.LoadTimeDepositsPort;
import org.ikigaidigital.application.port.out.LoadWithdrawalsPort;
import org.ikigaidigital.application.port.out.SaveTimeDepositsPort;
import org.ikigaidigital.domain.Withdrawal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements both use cases. Depends only on ports and the legacy calculator, never on adapters. Spring's
 * {@code @Service}/{@code @Transactional} are allowed in the application layer (ADR 0005); the domain and legacy
 * classes stay framework-free.
 */
@Service
public class TimeDepositService implements ListTimeDepositsUseCase, UpdateAllBalancesUseCase {

    private final LoadTimeDepositsPort loadTimeDeposits;
    private final SaveTimeDepositsPort saveTimeDeposits;
    private final LoadWithdrawalsPort loadWithdrawals;
    private final TimeDepositCalculator calculator;

    public TimeDepositService(LoadTimeDepositsPort loadTimeDeposits,
                              SaveTimeDepositsPort saveTimeDeposits,
                              LoadWithdrawalsPort loadWithdrawals,
                              TimeDepositCalculator calculator) {
        this.loadTimeDeposits = loadTimeDeposits;
        this.saveTimeDeposits = saveTimeDeposits;
        this.loadWithdrawals = loadWithdrawals;
        this.calculator = calculator;
    }

    /** Load, calculate and save in one transaction, so a failure never leaves some balances updated and others not. */
    @Override
    @Transactional
    public void updateAllBalances() {
        List<TimeDeposit> timeDeposits = loadTimeDeposits.loadAllTimeDeposits();
        calculator.updateBalance(timeDeposits);
        saveTimeDeposits.saveAll(timeDeposits);
    }

    /** Two queries (deposits, withdrawals) combined in memory: no N+1 per deposit. */
    @Override
    @Transactional(readOnly = true)
    public List<TimeDepositView> listTimeDeposits() {
        Map<Integer, List<Withdrawal>> withdrawalsByDeposit = loadWithdrawals.loadAllWithdrawals().stream()
                .collect(Collectors.groupingBy(Withdrawal::timeDepositId));

        return loadTimeDeposits.loadAllTimeDeposits().stream()
                .map(deposit -> new TimeDepositView(
                        deposit.getId(),
                        deposit.getPlanType(),
                        deposit.getBalance(),
                        deposit.getDays(),
                        withdrawalsByDeposit.getOrDefault(deposit.getId(), List.of())))
                .toList();
    }
}
