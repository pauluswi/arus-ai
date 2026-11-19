package com.pswied.core.investigation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvestigationRepository extends JpaRepository<Investigation, UUID> {
}
