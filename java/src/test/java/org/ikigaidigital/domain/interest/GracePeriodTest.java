package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * The 30-day grace period on its own, independent of any plan. This boundary can't be observed for premium through
 * {@code updateBalance}, because premium's own day-45 rule hides it.
 */
class GracePeriodTest {

    /** A delegate that always earns 7.0 and counts calls, so the test can tell whether the decorator consulted it. */
    private static final class AlwaysEarns implements InterestPolicy {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public String planType() {
            return "any-plan";
        }

        @Override
        public double monthlyInterest(TimeDeposit deposit) {
            calls.incrementAndGet();
            return 7.0;
        }
    }

    private final AlwaysEarns delegate = new AlwaysEarns();
    private final InterestPolicy withGracePeriod = new GracePeriod(delegate);

    @ParameterizedTest(name = "day {0}: no interest, delegate not asked")
    @CsvSource({"0", "1", "30"})
    void appliesNoInterestUpToAndIncludingDay30(int days) {
        assertThat(withGracePeriod.monthlyInterest(deposit(days))).isEqualTo(0.0);
        assertThat(delegate.calls).hasValue(0);
    }

    @ParameterizedTest(name = "day {0}: delegate decides")
    @CsvSource({"31", "46", "2147483647"})
    void delegatesFromDay31(int days) {
        assertThat(withGracePeriod.monthlyInterest(deposit(days))).isEqualTo(7.0);
        assertThat(delegate.calls).hasValue(1);
    }

    @Test
    void keepsTheDelegatesPlanType() {
        assertThat(withGracePeriod.planType()).isEqualTo("any-plan");
    }

    @Test
    void requiresADelegate() {
        assertThatNullPointerException().isThrownBy(() -> new GracePeriod(null));
    }

    private static TimeDeposit deposit(int days) {
        return new TimeDeposit(1, "any-plan", 1000.00, days);
    }
}
