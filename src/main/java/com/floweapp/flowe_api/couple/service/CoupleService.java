package com.floweapp.flowe_api.couple.service;

import com.floweapp.flowe_api.couple.dto.CoupleMemberResponseDto;
import com.floweapp.flowe_api.couple.dto.CoupleResponseDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
import com.floweapp.flowe_api.couple.dto.JoinCoupleRequestDto;
import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.entity.CoupleStatus;
import com.floweapp.flowe_api.couple.entity.InviteCode;
import com.floweapp.flowe_api.couple.exception.*;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.couple.repository.InviteCodeRepository;
import com.floweapp.flowe_api.user.entity.User;
import com.floweapp.flowe_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final InviteCodeGenerator inviteCodeGenerator;

    @Transactional
    public CoupleResponseDto createCouple(User currentUser, CoupleNameRequestDto request) {
        UUID userId = currentUser.getId();

        if (coupleRepository.existsByUser1Id(userId) ||
                coupleRepository.existsByUser2Id(userId)) {
            throw new CoupleAlreadyExistsException();
        }

        String name = request.name();
        Couple couple = Couple.builder()
                .name(name)
                .status(CoupleStatus.pending)
                .user1Id(userId)
                .user2Id(null)
                .build();

        Couple saved = coupleRepository.save(couple);

        String code = generateUniqueInviteCode();
        InviteCode inviteCode = InviteCode.builder()
                .coupleId(saved.getId())
                .inviteCode(code)
                .build();
        inviteCodeRepository.save(inviteCode);

        return toResponse(saved, userId, code);
    }

    @Transactional(readOnly = true)
    public CoupleResponseDto getMyCouple(User currentUser) {
        Couple couple = coupleRepository.findByUserId(currentUser.getId())
                .orElseThrow(CoupleNotFoundException::new);

        String inviteCode = null;
        if (couple.isPending()) {
            inviteCode = inviteCodeRepository.findByCoupleId(couple.getId())
                    .map(InviteCode::getInviteCode)
                    .orElse(null);
        }

        return toResponse(couple, currentUser.getId(), inviteCode);
    }

    @Transactional
    public CoupleResponseDto updateMyCouple(User currentUser, CoupleNameRequestDto request) {
        Couple couple = coupleRepository.findByUserId(currentUser.getId())
                .orElseThrow(CoupleNotFoundException::new);

        couple.setName(request.name());
        Couple saved = coupleRepository.save(couple);

        String inviteCode = null;
        if (saved.isPending()) {
            inviteCode = inviteCodeRepository.findByCoupleId(saved.getId())
                    .map(InviteCode::getInviteCode)
                    .orElse(null);
        }

        return toResponse(saved, currentUser.getId(), inviteCode);
    }

    @Transactional
    public CoupleResponseDto joinCouple(User currentUser, JoinCoupleRequestDto request) {
        UUID userId = currentUser.getId();

        InviteCode inviteCode = inviteCodeRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(InviteCodeNotFoundException::new);

        Couple couple = coupleRepository.findByIdForUpdate(inviteCode.getCoupleId())
                .orElseThrow(InviteCodeNotFoundException::new);

        if (couple.isActive() || couple.getUser2Id() != null) {
            throw new CoupleAlreadyJoinedException();
        }

        if (couple.getUser1Id().equals(userId)) {
            throw new CannotJoinOwnCoupleException();
        }

        if (coupleRepository.existsByUser1Id(userId) || coupleRepository.existsByUser2Id(userId)) {
            throw new CoupleAlreadyExistsException();
        }

        couple.setUser2Id(userId);
        couple.setStatus(CoupleStatus.active);
        Couple saved = coupleRepository.save(couple);

        inviteCodeRepository.delete(inviteCode);

        return toResponse(saved, userId, null);
    }

    @Transactional(readOnly = true)
    public List<CoupleMemberResponseDto> getMembers(User currentUser) {
        Couple couple = coupleRepository.findByUserId(currentUser.getId())
                .orElseThrow(CoupleNotFoundException::new);

        if (!couple.isActive()) {
            throw new CoupleNotActiveException();
        }

        UUID currentUserId = currentUser.getId();

        User user1 = userRepository.findById(couple.getUser1Id())
                .orElseThrow(() -> new IllegalStateException("User1 не найден"));
        User user2 = userRepository.findById(couple.getUser2Id())
                .orElseThrow(() -> new IllegalStateException("User2 не найден"));

        return List.of(
                new CoupleMemberResponseDto(
                        user1.getId(),
                        user1.getDisplayName(),
                        user1.getId().equals(currentUserId)
                ),
                new CoupleMemberResponseDto(
                        user2.getId(),
                        user2.getDisplayName(),
                        user2.getId().equals(currentUserId)
                )
        );
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = inviteCodeGenerator.generate();
            if (!inviteCodeRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new InviteCodeGenerationException();
    }

    private CoupleResponseDto toResponse(Couple couple, UUID currentUserId ,String inviteCode) {
        String partnerName = null;

        if (couple.isActive()) {
            UUID partnerId = couple.getUser1Id().equals(currentUserId)
                    ? couple.getUser2Id()
                    : couple.getUser1Id();

            partnerName = userRepository.findById(partnerId)
                    .map(User::getDisplayName)
                    .orElse(null);
        }

        return new CoupleResponseDto(
                couple.getId(),
                couple.getName(),
                couple.getStatus(),
                partnerName,
                inviteCode
        );
    }
}
