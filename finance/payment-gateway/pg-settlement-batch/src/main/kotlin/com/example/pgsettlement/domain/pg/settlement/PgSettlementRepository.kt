package com.example.pgsettlement.domain.pg.settlement

import org.springframework.data.jpa.repository.JpaRepository

interface PgSettlementRepository : JpaRepository<PgSettlementEntity, String>
