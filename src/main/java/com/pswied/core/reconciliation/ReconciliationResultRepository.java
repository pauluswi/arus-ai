package com.pswied.core.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, UUID> {

    List<ReconciliationResult> findByBatchIdOrderByCreatedAtAscIdAsc(UUID batchId);

    List<ReconciliationResult> findByBatchIdInOrderByCreatedAtAscIdAsc(Collection<UUID> batchIds);

    List<ReconciliationResult> findByInternalRecordIdInOrExternalRecordIdIn(
            Collection<UUID> internalRecordIds,
            Collection<UUID> externalRecordIds);
}
