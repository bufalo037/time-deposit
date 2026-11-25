package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

/**
 * Student plan: 3% a year, applied monthly, with no interest after 1 year (README).
 * "1 year" is pinned by the legacy code and the characterization tests: day 365 still earns, day 366 doesn't.
 */
public final class StudentInterestPolicy implements InterestPolicy {

    public static final String PLAN_TYPE = "student";
    private static final double ANNUAL_RATE = 0.03;
    private static final int MONTHS_PER_YEAR = 12;
    private static final int LAST_DAY_WITH_INTEREST = 365;

    @Override
    public String planType() {
        return PLAN_TYPE;
    }

    /** Keep the legacy evaluation order {@code (balance * rate) / 12}: folding the rate changes the double result. */
    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() > LAST_DAY_WITH_INTEREST) {
            return 0.0;
        }
        return deposit.getBalance() * ANNUAL_RATE / MONTHS_PER_YEAR;
    }
}
