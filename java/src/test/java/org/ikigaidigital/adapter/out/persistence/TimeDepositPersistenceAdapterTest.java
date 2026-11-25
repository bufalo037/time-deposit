package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.PostgresTestcontainersConfiguration;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Persistence adapter against real PostgreSQL (Testcontainers). Flyway applies the real migrations, so these tests
 * also prove the schema: column mapping, NOT NULL constraints, the foreign key and money precision.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestcontainersConfiguration.class, TimeDepositPersistenceAdapter.class})
class TimeDepositPersistenceAdapterTest {

    @Autowired
    private TimeDepositPersistenceAdapter adapter;

    @Autowired
    private TimeDepositJpaRepository timeDepositRepository;

    @Autowired
    private WithdrawalJpaRepository withdrawalRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    class LoadingTimeDeposits {

        @Test
        void mapsEveryRowToATimeDepositOrderedById() {
            int premiumId = persistDeposit("premium", 90, "20000.00");
            int basicId = persistDeposit("basic", 20, "10000.50");

            List<TimeDeposit> deposits = adapter.loadAllTimeDeposits();

            assertThat(deposits)
                    .extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getDays, TimeDeposit::getBalance)
                    .containsExactly(
                            tuple(premiumId, "premium", 90, 20000.00),
                            tuple(basicId, "basic", 20, 10000.50));
        }

        @Test
        void keepsIdOrderAfterBalancesAreUpdated() {
            // A PostgreSQL UPDATE writes a new row version at the end of the table, so without ORDER BY the updated
            // row would come back last. Listing after "update all balances" must still be ordered by id.
            int first = persistDeposit("basic", 31, "1000.00");
            int second = persistDeposit("basic", 31, "2000.00");
            adapter.saveAll(List.of(new TimeDeposit(first, "basic", 1000.83, 31)));
            entityManager.flush();
            entityManager.clear();

            assertThat(adapter.loadAllTimeDeposits())
                    .extracting(TimeDeposit::getId)
                    .containsExactly(first, second);
        }

        @Test
        void returnsAnEmptyListWhenThereAreNoDeposits() {
            assertThat(adapter.loadAllTimeDeposits()).isEmpty();
        }
    }

    @Nested
    class SavingBalances {

        @Test
        void storesTheUpdatedBalanceAndLeavesPlanTypeAndDaysUnchanged() {
            int id = persistDeposit("premium", 46, "1000.00");

            adapter.saveAll(List.of(new TimeDeposit(id, "premium", 1004.17, 46)));
            entityManager.flush();
            entityManager.clear();

            TimeDepositJpaEntity stored = timeDepositRepository.findById(id).orElseThrow();
            assertThat(stored.getBalance()).isEqualByComparingTo("1004.17");
            assertThat(stored.getPlanType()).isEqualTo("premium");
            assertThat(stored.getDays()).isEqualTo(46);
        }

        @Test
        void roundsDoubleArtifactsToCentsBeforeStoring() {
            // updateBalance(basic, 1000.10, 60 days) produces 1000.9300000000001 (pinned by the characterization tests)
            int id = persistDeposit("basic", 60, "1000.10");

            adapter.saveAll(List.of(new TimeDeposit(id, "basic", 1000.9300000000001, 60)));
            entityManager.flush();
            entityManager.clear();

            assertThat(timeDepositRepository.findById(id).orElseThrow().getBalance())
                    .isEqualTo(new BigDecimal("1000.93"));
        }

        @Test
        void roundsHalfACentUpUsingTheDecimalValueOfTheDouble() {
            // The double 1004.165 is stored as 1004.16499999999996... Rounding its exact binary value would give
            // 1004.16. The adapter rounds the decimal the double represents (BigDecimal.valueOf), giving 1004.17.
            int id = persistDeposit("premium", 60, "1000.00");

            adapter.saveAll(List.of(new TimeDeposit(id, "premium", 1004.165, 60)));
            entityManager.flush();
            entityManager.clear();

            assertThat(timeDepositRepository.findById(id).orElseThrow().getBalance())
                    .isEqualTo(new BigDecimal("1004.17"));
        }

        @Test
        void savesEveryGivenDepositAndIgnoresUnknownIds() {
            int first = persistDeposit("basic", 31, "1000.00");
            int second = persistDeposit("student", 31, "1000.00");

            adapter.saveAll(List.of(
                    new TimeDeposit(first, "basic", 1000.83, 31),
                    new TimeDeposit(second, "student", 1002.5, 31),
                    new TimeDeposit(999_999, "basic", 1.0, 31)));
            entityManager.flush();
            entityManager.clear();

            assertThat(adapter.loadAllTimeDeposits())
                    .extracting(TimeDeposit::getId, TimeDeposit::getBalance)
                    .containsExactly(tuple(first, 1000.83), tuple(second, 1002.5));
        }
    }

    @Nested
    class LoadingWithdrawals {

        @Test
        void mapsEveryRowToAWithdrawalOrderedByDepositThenDate() {
            int second = persistDeposit("premium", 90, "20000.00");
            int first = persistDeposit("basic", 120, "10000.00");
            persistWithdrawal(second, "250.00", LocalDate.of(2026, 10, 1));
            persistWithdrawal(first, "500.00", LocalDate.of(2026, 9, 1));
            persistWithdrawal(second, "100.00", LocalDate.of(2026, 8, 15));

            List<Withdrawal> withdrawals = adapter.loadAllWithdrawals();

            assertThat(withdrawals)
                    .extracting(Withdrawal::timeDepositId, Withdrawal::amount, Withdrawal::date)
                    .containsExactly(
                            tuple(second, new BigDecimal("100.00"), LocalDate.of(2026, 8, 15)),
                            tuple(second, new BigDecimal("250.00"), LocalDate.of(2026, 10, 1)),
                            tuple(first, new BigDecimal("500.00"), LocalDate.of(2026, 9, 1)));
        }
    }

    @Nested
    class SchemaConstraints {

        @Test
        void rejectsATimeDepositWithoutPlanType() {
            assertThatThrownBy(() -> timeDepositRepository.saveAndFlush(
                    new TimeDepositJpaEntity(null, 31, new BigDecimal("1000.00"))))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void rejectsAWithdrawalForANonExistentTimeDeposit() {
            assertThatThrownBy(() -> withdrawalRepository.saveAndFlush(
                    new WithdrawalJpaEntity(999_999, new BigDecimal("10.00"), LocalDate.of(2026, 1, 1))))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("fk_withdrawals_time_deposits");
        }

        @Test
        void storesMoneyAtCentPrecision() {
            int id = persistDeposit("basic", 31, "1234567.891");

            entityManager.clear();

            assertThat(timeDepositRepository.findById(id).orElseThrow().getBalance())
                    .isEqualTo(new BigDecimal("1234567.89"));
        }
    }

    private int persistDeposit(String planType, int days, String balance) {
        TimeDepositJpaEntity entity = timeDepositRepository.saveAndFlush(
                new TimeDepositJpaEntity(planType, days, new BigDecimal(balance)));
        return entity.getId();
    }

    private void persistWithdrawal(int timeDepositId, String amount, LocalDate date) {
        withdrawalRepository.saveAndFlush(new WithdrawalJpaEntity(timeDepositId, new BigDecimal(amount), date));
    }
}
