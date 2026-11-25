package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

/**
 * Strategy for one plan type's monthly interest (ADR 0008).
 *
 * <p>Contract, so every implementation is substitutable (LSP):
 * <ul>
 *   <li>{@link #planType()} is the exact, case-sensitive plan type it handles (e.g. {@code "basic"}).</li>
 *   <li>{@link #monthlyInterest(TimeDeposit)} returns the RAW monthly interest as a {@code double}: not rounded, and
 *       {@code 0.0} when the deposit isn't eligible. Rounding and applying it to the balance are the calculator's
 *       job.</li>
 *   <li>It never modifies the deposit.</li>
 * </ul>
 * To add a plan, add a new implementation and register it in {@link InterestPolicies#defaults()}. Existing policies
 * and {@code TimeDepositCalculator} stay unchanged (OCP).
 */
public interface InterestPolicy {

    String planType();

    double monthlyInterest(TimeDeposit deposit);
}
