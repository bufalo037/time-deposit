package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.port.out.LoadTimeDepositsPort;
import org.ikigaidigital.application.port.out.LoadWithdrawalsPort;
import org.ikigaidigital.application.port.out.SaveTimeDepositsPort;
import org.ikigaidigital.domain.Withdrawal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Persistence adapter: implements the outgoing ports with Spring Data JPA on PostgreSQL. */
@Component
class TimeDepositPersistenceAdapter implements LoadTimeDepositsPort, SaveTimeDepositsPort, LoadWithdrawalsPort {

    private final TimeDepositJpaRepository timeDepositRepository;
    private final WithdrawalJpaRepository withdrawalRepository;

    TimeDepositPersistenceAdapter(TimeDepositJpaRepository timeDepositRepository,
                                  WithdrawalJpaRepository withdrawalRepository) {
        this.timeDepositRepository = timeDepositRepository;
        this.withdrawalRepository = withdrawalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeDeposit> loadAllTimeDeposits() {
        return timeDepositRepository.findAllByOrderByIdAsc().stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void saveAll(List<TimeDeposit> timeDeposits) {
        Map<Integer, TimeDeposit> byId = timeDeposits.stream()
                .collect(Collectors.toMap(TimeDeposit::getId, Function.identity()));

        // Only the balance is written back: updateBalance never changes id, plan type or days.
        // Unknown ids are ignored (README: invalid input handling isn't required).
        timeDepositRepository.findAllById(byId.keySet())
                .forEach(entity -> entity.setBalance(
                        PersistenceMapper.toStoredMoney(byId.get(entity.getId()).getBalance())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Withdrawal> loadAllWithdrawals() {
        return withdrawalRepository.findAllByOrderByTimeDepositIdAscDateAscIdAsc().stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }
}
