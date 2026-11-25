package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** The policy registry and the extension point it gives {@link TimeDepositCalculator}. */
class InterestPoliciesTest {

    @Nested
    class Defaults {

        private final InterestPolicies defaults = InterestPolicies.defaults();

        @Test
        void containsExactlyTheThreeExistingPlans() {
            assertThat(defaults.planTypes()).containsExactlyInAnyOrder("basic", "student", "premium");
        }

        @ParameterizedTest(name = "{0} at day 30 -> no interest (grace period)")
        @CsvSource({"basic", "student", "premium"})
        void applyTheGracePeriodToEveryExistingPlan(String planType) {
            assertThat(defaults.monthlyInterestFor(new TimeDeposit(1, planType, 1000.00, 30))).isEqualTo(0.0);
        }

        @ParameterizedTest(name = "{0} at day {1} -> {2}")
        @CsvSource({"basic, 31, 0.8333333333333334", "student, 31, 2.5", "premium, 46, 4.166666666666667"})
        void delegateToThePlansPolicyAfterTheGracePeriod(String planType, int days, double expected) {
            assertThat(defaults.monthlyInterestFor(new TimeDeposit(1, planType, 1000.00, days))).isEqualTo(expected);
        }

        @ParameterizedTest(name = "plan ''{0}'' -> no interest")
        @CsvSource({"gold", "Basic", "PREMIUM", "''"})
        void giveNoInterestToUnknownOrDifferentlyCasedPlanTypes(String planType) {
            assertThat(defaults.monthlyInterestFor(new TimeDeposit(1, planType, 1000.00, 100))).isEqualTo(0.0);
        }

        @Test
        void giveNoInterestToANullPlanTypeInsteadOfFailing() {
            // Invalid input is out of scope (README). Documented in ADR 0008: a null plan type simply has no policy.
            assertThat(defaults.monthlyInterestFor(new TimeDeposit(1, null, 1000.00, 100))).isEqualTo(0.0);
        }
    }

    @Test
    void rejectsTwoPoliciesForTheSamePlanType() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InterestPolicies(List.of(new BasicInterestPolicy(), new BasicInterestPolicy())))
                .withMessageContaining("basic");
    }

    @Test
    void anAdditionalPlanNeedsOnlyANewPolicyAndNoChangeToTheCalculator() {
        // Open/closed: a hypothetical plan, defined only here, is picked up by the unchanged calculator, and the
        // calculator's rounding still applies.
        InterestPolicy gold = new InterestPolicy() {
            @Override
            public String planType() {
                return "gold";
            }

            @Override
            public double monthlyInterest(TimeDeposit deposit) {
                return deposit.getBalance() * 0.07 / 12;
            }
        };
        InterestPolicies withGold = new InterestPolicies(List.of(new GracePeriod(new BasicInterestPolicy()), gold));
        TimeDeposit goldDeposit = new TimeDeposit(1, "gold", 1000.00, 10);
        TimeDeposit basicDeposit = new TimeDeposit(2, "basic", 1000.00, 31);

        new TimeDepositCalculator(withGold).updateBalance(List.of(goldDeposit, basicDeposit));

        assertThat(goldDeposit.getBalance()).isEqualTo(1005.83);   // 5.8333.. rounded; no grace period chosen for gold
        assertThat(basicDeposit.getBalance()).isEqualTo(1000.83);
    }
}
