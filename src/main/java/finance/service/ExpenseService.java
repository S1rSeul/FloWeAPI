package finance.service;

import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.exception.CoupleNotActiveException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.user.entity.User;
import finance.dto.CreateExpenseRequestDto;
import finance.dto.ExpenseResponseDto;
import finance.entity.Expense;
import finance.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CoupleRepository coupleRepository;

    @Transactional
    public ExpenseResponseDto createExpense(User currentUser, CreateExpenseRequestDto request) {
        Couple couple = getActiveCoupleOrThrow(currentUser.getId());
        UUID currentUserId = currentUser.getId();

        BigDecimal totalPaid = request.myPaidAmount().add(request.partnerPaidAmount());
        if (totalPaid.compareTo(request.amount()) != 0) {
            throw new IllegalArgumentException("Сумма вкладов партнеров в чек должна быть равна общей сумме");
        }

        boolean isCurrentUserUser1 = couple.getUser1Id().equals(currentUserId);

        BigDecimal user1Paid = isCurrentUserUser1 ? request.myPaidAmount() : request.partnerPaidAmount();
        BigDecimal user2Paid = isCurrentUserUser1 ? request.partnerPaidAmount() : request.myPaidAmount();

        Expense expense = Expense.builder()
                .coupleId(couple.getId())
                .createdById(currentUserId)
                .amount(request.amount())
                .user1PaidAmount(user1Paid)
                .user2PaidAmount(user2Paid)
                .currency(request.currency())
                .category(request.category())
                .spentAt(request.spentAt())
                .description(request.description())
                .build();

        Expense saved = expenseRepository.save(expense);

        return ExpenseResponseDto.from(saved, currentUserId, couple.getUser1Id());
    }

    public Couple getActiveCoupleOrThrow(UUID userId) {
        Couple couple = coupleRepository.findByUserId(userId)
                .orElseThrow(CoupleNotFoundException::new);
        if (!couple.isActive()) {
            throw new CoupleNotActiveException();
        }
        return couple;
    }
}
