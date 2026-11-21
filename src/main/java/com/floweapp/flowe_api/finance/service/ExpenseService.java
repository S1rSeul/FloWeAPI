package com.floweapp.flowe_api.finance.service;

import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.exception.CoupleNotActiveException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.finance.dto.*;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import com.floweapp.flowe_api.finance.repository.ExpenseSpecification;
import com.floweapp.flowe_api.task.exception.InvalidQueryParameterException;
import com.floweapp.flowe_api.user.entity.User;
import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.Expense;
import com.floweapp.flowe_api.finance.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public ExpenseHistoryResponseDto getExpenses(
            User currentUser,
            OffsetDateTime from,
            OffsetDateTime to,
            ExpenseCategory category,
            Integer limit,
            String cursor
    ) {
        Couple couple = getActiveCoupleOrThrow(currentUser.getId());
        int pageSize = limit == null ? 20 : limit;

        if (pageSize < 1 || pageSize > 100) {
            throw new InvalidQueryParameterException("limit должен быть от 1 до 100");
        }

        OffsetDateTime fromOffset = from != null ? from : OffsetDateTime.parse("1970-01-01T00:00:00Z");
        OffsetDateTime toOffset = to != null ? to : OffsetDateTime.parse("9999-12-31T23:59:59Z");

        OffsetDateTime cursorAt = null;
        UUID cursorId = null;
        boolean hasCursor = cursor != null && !cursor.isBlank();
        if (hasCursor) {
            ExpenseCursor.Decoded decoded = ExpenseCursor.decode(cursor);
            cursorAt = decoded.spentAt();
            cursorId = decoded.id();
        }

        Specification<Expense> spec = Specification.where(ExpenseSpecification.byCouple(couple.getId()))
                .and(ExpenseSpecification.byDateRange(from != null ? fromOffset : null, to != null ? toOffset : null))
                .and(ExpenseSpecification.byCategory(category))
                .and(ExpenseSpecification.byCursor(cursorAt, cursorId));

        PageRequest pageRequest = PageRequest.of(0, pageSize + 1, Sort.by(Sort.Order.desc("spentAt"), Sort.Order.desc("id")));
        Page<Expense> page = expenseRepository.findAll(spec, pageRequest);
        List<Expense> expenses = new ArrayList<>(page.getContent());

        boolean hasMore = expenses.size() > pageSize;
        if (hasMore) {
            expenses = expenses.subList(0, pageSize);
        }

        String nextCursor = null;
        if (hasMore && !expenses.isEmpty()) {
            Expense last = expenses.getLast();
            nextCursor = ExpenseCursor.encode(last.getSpentAt(), last.getId());
        }

        List<Object[]> rawTotals;
        if (category != null) {
            rawTotals = expenseRepository.getTotalSummaryWithCategory(
                    couple.getId(), fromOffset, toOffset, category
            );
        } else {
            rawTotals = expenseRepository.getTotalSummaryNoCategory(
                    couple.getId(), fromOffset, toOffset
            );
        }

        boolean isCurrentUserUser1 = couple.getUser1Id().equals(currentUser.getId());
        List<CurrencySummaryDto> totalSummary = new ArrayList<>();

        for (Object[] row : rawTotals) {
            CurrencyCode currency = (CurrencyCode) row[0];
            BigDecimal totalAmount = (BigDecimal) row[1];
            BigDecimal u1Paid = (BigDecimal) row[2];
            BigDecimal u2Paid = (BigDecimal) row[3];

            BigDecimal myPaid = isCurrentUserUser1 ? u1Paid : u2Paid;
            BigDecimal partnerPaid = isCurrentUserUser1 ? u2Paid : u1Paid;

            totalSummary.add(new CurrencySummaryDto(currency, totalAmount, myPaid, partnerPaid));
        }

        List<ExpenseResponseDto> items = expenses.stream()
                .map(e -> ExpenseResponseDto.from(e, currentUser.getId(), couple.getUser1Id()))
                .toList();

        return new ExpenseHistoryResponseDto(items, nextCursor, hasMore, totalSummary);
    }

    private Couple getActiveCoupleOrThrow(UUID userId) {
        Couple couple = coupleRepository.findByUserId(userId)
                .orElseThrow(CoupleNotFoundException::new);
        if (!couple.isActive()) {
            throw new CoupleNotActiveException();
        }
        return couple;
    }
}
