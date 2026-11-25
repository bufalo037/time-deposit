package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

/** Basic plan: 1% a year, applied monthly, with no upper day limit. The 30-day grace period is {@link GracePeriod}'s job. */
public final class BasicInterestPolicy implements InterestPolicy {

    public static final String PLAN_TYPE = "basic";
    private static final double ANNUAL_RATE = 0.01;
    private static final int MONTHS_PER_YEAR = 12;

    @Override
    public String planType() {
        return PLAN_TYPE;
    }

    /** Keep the legacy evaluation order {@code (balance * rate) / 12}: folding the rate changes the double result. */
    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        return deposit.getBalance() * ANNUAL_RATE / MONTHS_PER_YEAR;
    }
}
