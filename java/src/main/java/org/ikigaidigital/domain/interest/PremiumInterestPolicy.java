package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

/**
 * Premium plan: 5% a year, applied monthly. Interest starts after 45 days (README): day 45 earns nothing, day 46
 * does. There's no upper day limit.
 */
public final class PremiumInterestPolicy implements InterestPolicy {

    public static final String PLAN_TYPE = "premium";
    private static final double ANNUAL_RATE = 0.05;
    private static final int MONTHS_PER_YEAR = 12;
    private static final int LAST_DAY_WITHOUT_INTEREST = 45;

    @Override
    public String planType() {
        return PLAN_TYPE;
    }

    /** Keep the legacy evaluation order {@code (balance * rate) / 12}: folding the rate changes the double result. */
    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= LAST_DAY_WITHOUT_INTEREST) {
            return 0.0;
        }
        return deposit.getBalance() * ANNUAL_RATE / MONTHS_PER_YEAR;
    }
}
