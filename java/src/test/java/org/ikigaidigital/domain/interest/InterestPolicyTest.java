package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the per-plan policies on their own, without the grace period. Expected raw interest values were
 * captured from the original arithmetic {@code balance * rate / 12}. The "operation order" cases pin balances where
 * folding the rate ({@code balance * (rate / 12)}) would give a different double.
 */
class InterestPolicyTest {

    @Nested
    class Basic {

        private final InterestPolicy policy = new BasicInterestPolicy();

        @Test
        void handlesTheBasicPlanType() {
            assertThat(policy.planType()).isEqualTo("basic");
        }

        @ParameterizedTest(name = "days {0} -> 1000.00 earns {1}")
        @CsvSource({"0, 0.8333333333333334", "31, 0.8333333333333334", "366, 0.8333333333333334",
                "2147483647, 0.8333333333333334"})
        void earnsOnePercentAYearMonthlyWithNoDayLimitOfItsOwn(int days, double expected) {
            assertThat(policy.monthlyInterest(deposit("basic", 1000.00, days))).isEqualTo(expected);
        }

        @Test
        void keepsTheLegacyOperationOrder() {
            // (1.01 * 0.01) / 12 = 8.416666666666667E-4, whereas 1.01 * (0.01 / 12) = 8.416666666666668E-4
            assertThat(policy.monthlyInterest(deposit("basic", 1.01, 60))).isEqualTo(8.416666666666667E-4);
        }
    }

    @Nested
    class Student {

        private final InterestPolicy policy = new StudentInterestPolicy();

        @Test
        void handlesTheStudentPlanType() {
            assertThat(policy.planType()).isEqualTo("student");
        }

        @ParameterizedTest(name = "days {0} -> 1000.00 earns 2.5")
        @CsvSource({"0", "31", "365"})
        void earnsThreePercentAYearMonthlyUpToAndIncludingDay365(int days) {
            assertThat(policy.monthlyInterest(deposit("student", 1000.00, days))).isEqualTo(2.5);
        }

        @ParameterizedTest(name = "days {0} -> no interest")
        @CsvSource({"366", "10000", "2147483647"})
        void earnsNothingAfterOneYear(int days) {
            assertThat(policy.monthlyInterest(deposit("student", 1000.00, days))).isEqualTo(0.0);
        }

        @Test
        void keepsTheLegacyOperationOrder() {
            // (1.02 * 0.03) / 12 = 0.0025499999999999997, whereas 1.02 * (0.03 / 12) = 0.00255
            assertThat(policy.monthlyInterest(deposit("student", 1.02, 60))).isEqualTo(0.0025499999999999997);
        }
    }

    @Nested
    class Premium {

        private final InterestPolicy policy = new PremiumInterestPolicy();

        @Test
        void handlesThePremiumPlanType() {
            assertThat(policy.planType()).isEqualTo("premium");
        }

        @ParameterizedTest(name = "days {0} -> no interest")
        @CsvSource({"0", "31", "45"})
        void earnsNothingUpToAndIncludingDay45(int days) {
            assertThat(policy.monthlyInterest(deposit("premium", 1000.00, days))).isEqualTo(0.0);
        }

        @ParameterizedTest(name = "days {0} -> 1000.00 earns 4.166666666666667")
        @CsvSource({"46", "366", "2147483647"})
        void earnsFivePercentAYearMonthlyFromDay46WithNoUpperLimit(int days) {
            assertThat(policy.monthlyInterest(deposit("premium", 1000.00, days))).isEqualTo(4.166666666666667);
        }

        @Test
        void keepsTheLegacyOperationOrder() {
            // (1.01 * 0.05) / 12 = 0.004208333333333334, whereas 1.01 * (0.05 / 12) = 0.004208333333333333
            assertThat(policy.monthlyInterest(deposit("premium", 1.01, 60))).isEqualTo(0.004208333333333334);
        }
    }

    @Test
    void policiesNeverModifyTheDeposit() {
        TimeDeposit deposit = deposit("premium", 1000.00, 46);

        new BasicInterestPolicy().monthlyInterest(deposit);
        new StudentInterestPolicy().monthlyInterest(deposit);
        new PremiumInterestPolicy().monthlyInterest(deposit);

        assertThat(deposit.getBalance()).isEqualTo(1000.00);
        assertThat(deposit.getDays()).isEqualTo(46);
        assertThat(deposit.getPlanType()).isEqualTo("premium");
    }

    private static TimeDeposit deposit(String planType, double balance, int days) {
        return new TimeDeposit(1, planType, balance, days);
    }
}
