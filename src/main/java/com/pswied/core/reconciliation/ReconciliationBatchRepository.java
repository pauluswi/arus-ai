package com.pswied.core.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationBatchRepository extends JpaRepository<ReconciliationBatch, UUID> {

    List<ReconciliationBatch> findAllByOrderByStartedAtDesc();
}
