package finance.dto;

import finance.entity.CurrencyCode;
import finance.entity.Expense;
import finance.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponseDto(
        UUID id,
        BigDecimal amount,
        BigDecimal myPaidAmount,
        BigDecimal partnerPaidAmount,
        CurrencyCode currency,
        ExpenseCategory category,
        OffsetDateTime spentAt,
        String description,
        OffsetDateTime createdAt
) {
    public static ExpenseResponseDto from(Expense expense, UUID currentUserId, UUID user1Id) {
        boolean isCurrentUserUser1 = user1Id.equals(currentUserId);

        BigDecimal myPaid = isCurrentUserUser1 ? expense.getUser1PaidAmount() : expense.getUser2PaidAmount();
        BigDecimal partnerPaid = isCurrentUserUser1 ? expense.getUser2PaidAmount() : expense.getUser1PaidAmount();

        return new ExpenseResponseDto(
                expense.getId(),
                expense.getAmount(),
                myPaid,
                partnerPaid,
                expense.getCurrency(),
                expense.getCategory(),
                expense.getSpentAt(),
                expense.getDescription(),
                expense.getCreatedAt()
        );
    }
}
