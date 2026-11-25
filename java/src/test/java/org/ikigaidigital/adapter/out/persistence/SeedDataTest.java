package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.PostgresTestcontainersConfiguration;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Verifies the V2 seed migration: deterministic sample data that demonstrates every plan's interest rule. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestcontainersConfiguration.class, TimeDepositPersistenceAdapter.class})
class SeedDataTest {

    @Autowired
    private TimeDepositPersistenceAdapter adapter;

    @Autowired
    private TimeDepositJpaRepository timeDepositRepository;

    @Autowired
    private WithdrawalJpaRepository withdrawalRepository;

    @Test
    void seedsSixTimeDepositsCoveringEveryPlanBeforeAndAfterInterestStarts() {
        assertThat(adapter.loadAllTimeDeposits())
                .extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getDays, TimeDeposit::getBalance)
                .containsExactly(
                        tuple(1, "basic", 20, 10000.00),
                        tuple(2, "basic", 120, 10000.00),
                        tuple(3, "student", 200, 5000.00),
                        tuple(4, "student", 400, 5000.00),
                        tuple(5, "premium", 40, 20000.00),
                        tuple(6, "premium", 90, 20000.00));
    }

    @Test
    void seedsWithdrawalsForDeposits2And3And6Only() {
        assertThat(adapter.loadAllWithdrawals())
                .extracting(Withdrawal::id, Withdrawal::timeDepositId, Withdrawal::amount, Withdrawal::date)
                .containsExactly(
                        tuple(1, 2, new BigDecimal("500.00"), LocalDate.of(2026, 9, 15)),
                        tuple(2, 3, new BigDecimal("250.00"), LocalDate.of(2026, 7, 1)),
                        tuple(3, 6, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 10)),
                        tuple(4, 6, new BigDecimal("500.00"), LocalDate.of(2026, 10, 20)));
    }

    @Test
    void updatingBalancesChangesOnlyTheDepositsPastTheirInterestStart() {
        // The purpose of the seed: "update all balances" visibly changes ids 2, 3 and 6 and leaves 1, 4 and 5.
        // Expected values were captured from the original TimeDepositCalculator.
        List<TimeDeposit> deposits = adapter.loadAllTimeDeposits();

        new TimeDepositCalculator().updateBalance(deposits);

        assertThat(deposits)
                .extracting(TimeDeposit::getId, TimeDeposit::getBalance)
                .containsExactly(
                        tuple(1, 10000.00),
                        tuple(2, 10008.33),
                        tuple(3, 5012.5),
                        tuple(4, 5000.00),
                        tuple(5, 20000.00),
                        tuple(6, 20083.33));
    }

    @Test
    void resetsIdentitySequencesSoNewRowsNeverCollideWithSeededIds() {
        // Without the setval() calls in V2, the identity sequences would still start at 1 and these inserts would
        // fail with a duplicate primary key. The exact next id isn't asserted: sequence values aren't rolled back,
        // so other tests sharing this database may already have advanced them.
        TimeDepositJpaEntity newDeposit = timeDepositRepository.saveAndFlush(
                new TimeDepositJpaEntity("basic", 1, new BigDecimal("1.00")));
        WithdrawalJpaEntity newWithdrawal = withdrawalRepository.saveAndFlush(
                new WithdrawalJpaEntity(1, new BigDecimal("1.00"), LocalDate.of(2026, 11, 1)));

        assertThat(newDeposit.getId()).isGreaterThan(6);
        assertThat(newWithdrawal.getId()).isGreaterThan(4);
    }
}
