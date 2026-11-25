package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry that resolves a deposit's plan type to its {@link InterestPolicy} (ADR 0008).
 * Matching is exact and case-sensitive, and an unknown plan type gets no interest. Both are pinned by the
 * characterization tests.
 */
public final class InterestPolicies {

    private final Map<String, InterestPolicy> byPlanType;

    public InterestPolicies(Collection<? extends InterestPolicy> policies) {
        Map<String, InterestPolicy> map = new HashMap<>();
        for (InterestPolicy policy : policies) {
            if (map.putIfAbsent(policy.planType(), policy) != null) {
                // A wiring mistake in code, not user input: two policies would compete for the same plan type.
                throw new IllegalArgumentException("Duplicate interest policy for plan type: " + policy.planType());
            }
        }
        // A HashMap (unlike Map.copyOf) allows get(null), so a null plan type simply has no policy.
        this.byPlanType = Collections.unmodifiableMap(map);
    }

    /** The existing plans (basic, student, premium), each with the 30-day {@link GracePeriod}. */
    public static InterestPolicies defaults() {
        return new InterestPolicies(List.of(
                new GracePeriod(new BasicInterestPolicy()),
                new GracePeriod(new StudentInterestPolicy()),
                new GracePeriod(new PremiumInterestPolicy())));
    }

    /** Raw (unrounded) monthly interest for the deposit, or {@code 0.0} if no policy handles its plan type. */
    public double monthlyInterestFor(TimeDeposit deposit) {
        InterestPolicy policy = byPlanType.get(deposit.getPlanType());
        return policy == null ? 0.0 : policy.monthlyInterest(deposit);
    }

    public Set<String> planTypes() {
        return byPlanType.keySet();
    }
}
