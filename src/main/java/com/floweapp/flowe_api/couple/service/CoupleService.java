package com.floweapp.flowe_api.couple.service;

import com.floweapp.flowe_api.couple.dto.CoupleResponseDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.entity.CoupleStatus;
import com.floweapp.flowe_api.couple.entity.InviteCode;
import com.floweapp.flowe_api.couple.exception.CoupleAlreadyExistsException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.exception.InviteCodeGenerationException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.couple.repository.InviteCodeRepository;
import com.floweapp.flowe_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private static final int MAX_CODE_ATTEMPTS = 5;

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

        return toResponse(saved, code);
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

        return toResponse(couple, inviteCode);
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

    private CoupleResponseDto toResponse(Couple couple, String inviteCode) {
        return new CoupleResponseDto(
                couple.getId(),
                couple.getName(),
                couple.getStatus(),
                couple.getUser1Id(),
                couple.getUser2Id(),
                inviteCode,
                couple.getCreatedAt()
        );
    }
}
