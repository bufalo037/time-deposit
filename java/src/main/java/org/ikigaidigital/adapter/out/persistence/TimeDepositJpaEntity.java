package org.ikigaidigital.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** JPA mapping of table {@code time_deposits} (V1 migration). Never leaves the persistence adapter. */
@Entity
@Table(name = "time_deposits")
class TimeDepositJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType;

    @Column(name = "days", nullable = false)
    private Integer days;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    protected TimeDepositJpaEntity() {
        // for JPA
    }

    TimeDepositJpaEntity(String planType, Integer days, BigDecimal balance) {
        this.planType = planType;
        this.days = days;
        this.balance = balance;
    }

    Integer getId() {
        return id;
    }

    String getPlanType() {
        return planType;
    }

    Integer getDays() {
        return days;
    }

    BigDecimal getBalance() {
        return balance;
    }

    void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
