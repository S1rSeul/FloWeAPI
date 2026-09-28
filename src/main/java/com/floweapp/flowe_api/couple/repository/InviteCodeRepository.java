package com.floweapp.flowe_api.couple.repository;

import com.floweapp.flowe_api.couple.entity.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InviteCodeRepository extends JpaRepository<InviteCode, UUID> {

    boolean existsByInviteCode(String inviteCode);

    Optional<InviteCode> findByCoupleId(UUID coupleId);

    Optional<InviteCode> findByInviteCode(String inviteCode);
}
