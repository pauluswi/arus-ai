package com.pswied.core.investigation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestigationRepository extends JpaRepository<Investigation, UUID> {

    Optional<Investigation> findByExceptionId(UUID exceptionId);

    List<Investigation> findAllByExceptionIdIn(Iterable<UUID> exceptionIds);
}
