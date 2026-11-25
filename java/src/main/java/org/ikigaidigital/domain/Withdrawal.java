package org.ikigaidigital.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A withdrawal made from a time deposit. It's historical data, exposed alongside the deposit by the read endpoint.
 * It isn't part of {@code TimeDeposit} (shared class, no breaking changes) and doesn't affect interest, because
 * the README defines interest only by plan type and days (ADR 0003).
 */
public record Withdrawal(int id, int timeDepositId, BigDecimal amount, LocalDate date) {
}
