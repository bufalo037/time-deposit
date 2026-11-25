package org.ikigaidigital;

import org.ikigaidigital.domain.interest.InterestPolicies;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Applies one month of interest to each time deposit. The interest itself comes from per-plan
 * {@link org.ikigaidigital.domain.interest.InterestPolicy InterestPolicy} strategies (ADR 0008).
 *
 * <p>README constraints: the {@code updateBalance(List<TimeDeposit>)} signature and its behaviour are unchanged, as the
 * frozen characterization tests prove. The no-arg constructor keeps working for existing callers.
 */
public class TimeDepositCalculator {

    private final InterestPolicies interestPolicies;

    /** Existing behaviour: the basic, student and premium plans, each with the 30-day grace period. */
    public TimeDepositCalculator() {
        this(InterestPolicies.defaults());
    }

    /** Extension point: calculate with a different set of interest policies (e.g. an additional plan). */
    public TimeDepositCalculator(InterestPolicies interestPolicies) {
        this.interestPolicies = Objects.requireNonNull(interestPolicies, "interestPolicies");
    }

    public void updateBalance(List<TimeDeposit> xs) {
        for (TimeDeposit deposit : xs) {
            double interest = interestPolicies.monthlyInterestFor(deposit);

            // Unchanged from the original implementation: round the raw interest's EXACT binary value
            // (new BigDecimal(double), not BigDecimal.valueOf) to 2 decimals, HALF_UP, and add it to the double balance.
            double a2d = deposit.getBalance() + (new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)).doubleValue();
            deposit.setBalance(a2d);
        }
    }
}
