package com.floweapp.flowe_api.finance.repository;

import com.floweapp.flowe_api.finance.entity.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, UUID> {
    List<SavingsAccount> findAllByCoupleIdOrderByCreatedAtAsc(UUID coupleId);
    Optional<SavingsAccount> findByIdAndCoupleId(UUID id, UUID coupleId);
    boolean existsByCoupleIdAndName(UUID coupleId, String name);
    long countByCoupleId(UUID coupleId);
}
