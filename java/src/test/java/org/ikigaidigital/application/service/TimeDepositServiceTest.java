package org.ikigaidigital.application.service;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.TimeDepositView;
import org.ikigaidigital.application.port.out.LoadTimeDepositsPort;
import org.ikigaidigital.application.port.out.LoadWithdrawalsPort;
import org.ikigaidigital.application.port.out.SaveTimeDepositsPort;
import org.ikigaidigital.domain.Withdrawal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the use cases: ports are mocked, but the REAL legacy {@link TimeDepositCalculator} is used, so the
 * tests prove the service delegates the interest calculation to it unchanged.
 */
class TimeDepositServiceTest {

    private LoadTimeDepositsPort loadTimeDeposits;
    private SaveTimeDepositsPort saveTimeDeposits;
    private LoadWithdrawalsPort loadWithdrawals;
    private TimeDepositService service;

    @BeforeEach
    void setUp() {
        loadTimeDeposits = mock(LoadTimeDepositsPort.class);
        saveTimeDeposits = mock(SaveTimeDepositsPort.class);
        loadWithdrawals = mock(LoadWithdrawalsPort.class);
        service = new TimeDepositService(loadTimeDeposits, saveTimeDeposits, loadWithdrawals, new TimeDepositCalculator());
    }

    @Nested
    class UpdateAllBalances {

        @Test
        void savesTheBalancesCalculatedByTheLegacyCalculator() {
            when(loadTimeDeposits.loadAllTimeDeposits()).thenReturn(List.of(
                    new TimeDeposit(1, "basic", 1000.00, 31),
                    new TimeDeposit(2, "student", 1000.00, 366),
                    new TimeDeposit(3, "premium", 1000.00, 46)));

            service.updateAllBalances();

            assertThat(savedDeposits())
                    .extracting(TimeDeposit::getId, TimeDeposit::getBalance)
                    .containsExactly(tuple(1, 1000.83), tuple(2, 1000.0), tuple(3, 1004.17));
        }

        @Test
        void loadsBeforeSavingAndNeverReadsWithdrawals() {
            when(loadTimeDeposits.loadAllTimeDeposits()).thenReturn(List.of(new TimeDeposit(1, "basic", 1000.00, 31)));

            service.updateAllBalances();

            var order = inOrder(loadTimeDeposits, saveTimeDeposits);
            order.verify(loadTimeDeposits).loadAllTimeDeposits();
            order.verify(saveTimeDeposits).saveAll(anyList());
            verify(loadWithdrawals, never()).loadAllWithdrawals();
        }

        @Test
        void keepsIdPlanTypeAndDaysOfTheSavedDeposits() {
            when(loadTimeDeposits.loadAllTimeDeposits()).thenReturn(List.of(new TimeDeposit(7, "premium", 1000.00, 46)));

            service.updateAllBalances();

            assertThat(savedDeposits())
                    .extracting(TimeDeposit::getId, TimeDeposit::getPlanType, TimeDeposit::getDays)
                    .containsExactly(tuple(7, "premium", 46));
        }

        @Test
        void savesAnEmptyListWhenThereAreNoDeposits() {
            when(loadTimeDeposits.loadAllTimeDeposits()).thenReturn(List.of());

            service.updateAllBalances();

            assertThat(savedDeposits()).isEmpty();
        }

        private List<TimeDeposit> savedDeposits() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TimeDeposit>> captor = ArgumentCaptor.forClass(List.class);
            verify(saveTimeDeposits).saveAll(captor.capture());
            return captor.getValue();
        }
    }

    @Nested
    class ListTimeDeposits {

        private final Withdrawal firstOfDeposit2 = withdrawal(1, 2, "500.00", LocalDate.of(2026, 9, 15));
        private final Withdrawal firstOfDeposit3 = withdrawal(2, 3, "250.00", LocalDate.of(2026, 7, 1));
        private final Withdrawal secondOfDeposit3 = withdrawal(3, 3, "100.00", LocalDate.of(2026, 8, 1));

        @Test
        void combinesEachDepositWithItsOwnWithdrawalsInOrder() {
            when(loadTimeDeposits.loadAllTimeDeposits()).thenReturn(List.of(
                    new TimeDeposit(1, "basic", 10000.00, 20),
                    new TimeDeposit(2, "basic", 10000.00, 120),
                    new TimeDeposit(3, "student", 5000.00, 200)));
            when(loadWithdrawals.loadAllWithdrawals()).thenReturn(List.of(firstOfDeposit2, firstOfDeposit3, secondOfDeposit3));

            List<TimeDepositView> views = service.listTimeDeposits();

            assertThat(views)
                    .extracting(TimeDepositView::id, TimeDepositView::planType, TimeDepositView::balance,
                            TimeDepositView::days, TimeDepositView::withdrawals)
                    .containsExactly(
                            tuple(1, "basic", 10000.00, 20, List.of()),
                            tuple(2, "basic", 10000.00, 120, List.of(firstOfDeposit2)),
                            tuple(3, "student", 5000.00, 200, List.of(firstOfDeposit3, secondOfDeposit3)));
        }

        @Test
        void returnsAnEmptyListWhenThereAreNoDeposits() {
            when(loadTimeDeposits.loadAllTimeDeposits()).thenReturn(List.of());
            when(loadWithdrawals.loadAllWithdrawals()).thenReturn(List.of());

            assertThat(service.listTimeDeposits()).isEmpty();
        }

        @Test
        void neverSavesAnything() {
            when(loadTimeDeposits.loadAllTimeDeposits()).thenReturn(List.of(new TimeDeposit(1, "basic", 1000.00, 31)));
            when(loadWithdrawals.loadAllWithdrawals()).thenReturn(List.of());

            service.listTimeDeposits();

            verify(saveTimeDeposits, never()).saveAll(anyList());
        }

        private static Withdrawal withdrawal(int id, int timeDepositId, String amount, LocalDate date) {
            return new Withdrawal(id, timeDepositId, new BigDecimal(amount), date);
        }
    }
}
