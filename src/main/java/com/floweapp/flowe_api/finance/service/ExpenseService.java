package com.floweapp.flowe_api.finance.service;

import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.exception.CoupleNotActiveException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.user.entity.User;
import com.floweapp.flowe_api.finance.dto.CreateExpenseRequestDto;
import com.floweapp.flowe_api.finance.dto.CurrencySummaryDto;
import com.floweapp.flowe_api.finance.dto.ExpenseResponseDto;
import com.floweapp.flowe_api.finance.dto.ExpenseSummaryResponseDto;
import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.Expense;
import com.floweapp.flowe_api.finance.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public ExpenseSummaryResponseDto getSummary(User currentUser, OffsetDateTime from, OffsetDateTime to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата 'from' не может быть после даты 'to'");
        }

        Couple couple = getActiveCoupleOrThrow(currentUser.getId());
        UUID currentUserId = currentUser.getId();
        boolean isCurrentUserUser1 = couple.getUser1Id().equals(currentUserId);

        List<Expense> expenses = expenseRepository.findByCoupleIdAndSpentAtBetween(
                couple.getId(),
                from,
                to
        );

        Map<CurrencyCode, List<Expense>> groupedByCurrency = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCurrency));

        List<CurrencySummaryDto> summaries = groupedByCurrency.entrySet().stream()
                .map(entry -> {
                    CurrencyCode currency = entry.getKey();
                    List<Expense> currencyExpenses = entry.getValue();

                    BigDecimal total = currencyExpenses.stream()
                            .map(Expense::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal user1Paid = currencyExpenses.stream()
                            .map(Expense::getUser1PaidAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal user2Paid = currencyExpenses.stream()
                            .map(Expense::getUser2PaidAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal myPaid = isCurrentUserUser1 ? user1Paid : user2Paid;
                    BigDecimal partnerPaid = isCurrentUserUser1 ? user2Paid : user1Paid;

                    return new CurrencySummaryDto(currency, total, myPaid, partnerPaid);
                })
                .toList();

        return new ExpenseSummaryResponseDto(from, to, summaries);
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
