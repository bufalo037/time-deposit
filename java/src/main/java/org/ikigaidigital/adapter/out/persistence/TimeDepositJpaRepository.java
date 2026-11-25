package org.ikigaidigital.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface TimeDepositJpaRepository extends JpaRepository<TimeDepositJpaEntity, Integer> {

    List<TimeDepositJpaEntity> findAllByOrderByIdAsc();
}
