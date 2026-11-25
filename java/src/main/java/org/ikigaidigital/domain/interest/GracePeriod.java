package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

import java.util.Objects;

/**
 * Decorator for the README rule "No interest is applied for the first 30 days for any existing plans": no interest
 * while {@code days <= 30}, otherwise the wrapped policy decides. Written once and applied to every existing plan in
 * {@link InterestPolicies#defaults()}. A future plan can choose not to use it (ADR 0008).
 */
public final class GracePeriod implements InterestPolicy {

    private static final int LAST_DAY_WITHOUT_INTEREST = 30;

    private final InterestPolicy delegate;

    public GracePeriod(InterestPolicy delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public String planType() {
        return delegate.planType();
    }

    @Override
    public double monthlyInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= LAST_DAY_WITHOUT_INTEREST) {
            return 0.0;
        }
        return delegate.monthlyInterest(deposit);
    }
}
