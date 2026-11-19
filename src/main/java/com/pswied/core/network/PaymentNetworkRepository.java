package com.pswied.core.network;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentNetworkRepository extends JpaRepository<PaymentNetwork, UUID> {
}
