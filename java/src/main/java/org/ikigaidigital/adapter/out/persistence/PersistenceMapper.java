package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.domain.Withdrawal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Maps between JPA entities and domain types. The only place where money crosses the double/BigDecimal boundary. */
final class PersistenceMapper {

    /** Money is stored at cent precision: numeric(19,2) (ADR 0004). */
    static final int MONEY_SCALE = 2;

    private PersistenceMapper() {
    }

    static TimeDeposit toDomain(TimeDepositJpaEntity entity) {
        return new TimeDeposit(entity.getId(), entity.getPlanType(), entity.getBalance().doubleValue(), entity.getDays());
    }

    static Withdrawal toDomain(WithdrawalJpaEntity entity) {
        return new Withdrawal(entity.getId(), entity.getTimeDepositId(), entity.getAmount(), entity.getDate());
    }

    /**
     * Converts the calculator's {@code double} balance to the stored cent value. {@code updateBalance} works on
     * doubles and can produce representation artifacts (e.g. 1000.9300000000001). Rounding HALF_UP to 2 decimals
     * here keeps those artifacts out of the database and gives the next update a clean cent amount. The calculation
     * itself is unchanged (ADR 0004). {@code BigDecimal.valueOf} uses the double's shortest decimal representation,
     * so 1000.9300000000001 becomes 1000.93, not a long binary expansion.
     */
    static BigDecimal toStoredMoney(double amount) {
        return BigDecimal.valueOf(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
