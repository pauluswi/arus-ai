package com.pswied.core.exception;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExceptionResolutionRequestRepository extends JpaRepository<ExceptionResolutionRequest, UUID> {

    Optional<ExceptionResolutionRequest> findFirstByExceptionIdAndStatusOrderByRequestedAtDesc(
            UUID exceptionId,
            ExceptionResolutionRequestStatus status);

    List<ExceptionResolutionRequest> findAllByStatusOrderByRequestedAtAsc(
            ExceptionResolutionRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resolutionRequest from ExceptionResolutionRequest resolutionRequest "
            + "where resolutionRequest.id = :id")
    Optional<ExceptionResolutionRequest> findByIdForUpdate(@Param("id") UUID id);
}
