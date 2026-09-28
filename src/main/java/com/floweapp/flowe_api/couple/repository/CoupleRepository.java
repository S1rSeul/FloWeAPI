package com.floweapp.flowe_api.couple.repository;

import com.floweapp.flowe_api.couple.entity.Couple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CoupleRepository extends JpaRepository<Couple, UUID> {

    boolean existsByUser1Id(UUID user1Id);

    boolean existsByUser2Id(UUID user1Id);

    @Query("SELECT c FROM Couple c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    Optional<Couple> findByUserId(@Param("userId") UUID userId);
}
