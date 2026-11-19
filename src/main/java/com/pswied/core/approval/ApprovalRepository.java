package com.pswied.core.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApprovalRepository extends JpaRepository<Approval, UUID> {
}
