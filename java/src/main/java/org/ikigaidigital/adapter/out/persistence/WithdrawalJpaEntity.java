package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA mapping of table {@code withdrawals} (V1 migration). The foreign key is mapped as a plain column rather than a
 * {@code @ManyToOne}: withdrawals are only read in bulk for the listing, so a lazy association would add complexity
 * (and N+1 risk) for no benefit. The database still enforces the foreign key.
 */
@Entity
@Table(name = "withdrawals")
class WithdrawalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "time_deposit_id", nullable = false)
    private Integer timeDepositId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    protected WithdrawalJpaEntity() {
        // for JPA
    }

    WithdrawalJpaEntity(Integer timeDepositId, BigDecimal amount, LocalDate date) {
        this.timeDepositId = timeDepositId;
        this.amount = amount;
        this.date = date;
    }

    Integer getId() {
        return id;
    }

    Integer getTimeDepositId() {
        return timeDepositId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    LocalDate getDate() {
        return date;
    }
}
