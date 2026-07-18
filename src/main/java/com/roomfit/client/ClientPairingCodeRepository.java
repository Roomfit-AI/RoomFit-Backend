package com.roomfit.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientPairingCodeRepository extends JpaRepository<ClientPairingCode, Long> {

    Optional<ClientPairingCode> findByClientId(String clientId);

    Optional<ClientPairingCode> findByCode(String code);

    boolean existsByCode(String code);
}
