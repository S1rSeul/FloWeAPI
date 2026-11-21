package com.floweapp.flowe_api.finance.service;

import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.exception.CoupleNotActiveException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.finance.dto.BudgetResponseDto;
import com.floweapp.flowe_api.finance.dto.CreateBudgetRequestDto;
import com.floweapp.flowe_api.finance.dto.UpdateBudgetRequestDto;
import com.floweapp.flowe_api.finance.entity.Budget;
import com.floweapp.flowe_api.finance.entity.BudgetPeriod;
import com.floweapp.flowe_api.finance.entity.SavingsAccount;
import com.floweapp.flowe_api.finance.exception.BudgetAlreadyExistsException;
import com.floweapp.flowe_api.finance.exception.BudgetNotFoundException;
import com.floweapp.flowe_api.finance.exception.InvalidBudgetException;
import com.floweapp.flowe_api.finance.exception.SavingsAccountNotFoundException;
import com.floweapp.flowe_api.finance.repository.BudgetRepository;
import com.floweapp.flowe_api.finance.repository.ExpenseRepository;
import com.floweapp.flowe_api.finance.repository.SavingsAccountRepository;
import com.floweapp.flowe_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final SavingsAccountRepository savingsRepository;
    private final CoupleRepository coupleRepository;

    @Transactional
    public BudgetResponseDto create(User user, CreateBudgetRequestDto request) {
        Couple couple = getActiveCoupleOrThrow(user.getId());
        BudgetPeriod period = BudgetPeriod.MONTHLY;
        String accountName = null;

        if (request.savingsAccountId() != null) {
            SavingsAccount account = savingsRepository
                    .findByIdAndCoupleId(request.savingsAccountId(), couple.getId())
                    .orElseThrow(SavingsAccountNotFoundException::new);

            if (account.getCurrency() != request.currency()) {
                throw new InvalidBudgetException(
                        "Валюта лимита должна совпадать с валютой накопительного счета");
            }
            if (budgetRepository.existsBySavingsAccountIdAndPeriod(account.getId(), period)) {
                throw new BudgetAlreadyExistsException("Лимит для этой цели уже установлен");
            }
            accountName = account.getName();
        } else if (budgetRepository.existsByCoupleIdAndCategoryAndCurrencyAndPeriod(
                couple.getId(), request.category(), request.currency(), period)) {
            throw new BudgetAlreadyExistsException("Лимит для этой категории и валюты уже установлен");
        }

        Budget saved = budgetRepository.save(Budget.builder()
                .coupleId(couple.getId())
                .category(request.category())
                .savingsAccountId(request.savingsAccountId())
                .limitAmount(request.limitAmount())
                .currency(request.currency())
                .period(period)
                .build());

        return toResponse(saved, currentMonth(), accountName);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponseDto> getAll(User user, YearMonth month) {
        Couple couple = getActiveCoupleOrThrow(user.getId());
        YearMonth target = month != null ? month : currentMonth();

        Map<UUID, String> accountNames = savingsRepository
                .findAllByCoupleIdOrderByCreatedAtAsc(couple.getId()).stream()
                .collect(Collectors.toMap(SavingsAccount::getId, SavingsAccount::getName));

        return budgetRepository.findAllByCoupleIdOrderByCreatedAtAsc(couple.getId()).stream()
                .map(b -> toResponse(b, target, accountNames.get(b.getSavingsAccountId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponseDto getById(User user, UUID id, YearMonth month) {
        Couple couple = getActiveCoupleOrThrow(user.getId());
        Budget budget = budgetRepository.findByIdAndCoupleId(id, couple.getId())
                .orElseThrow(BudgetNotFoundException::new);
        return toResponse(budget, month != null ? month : currentMonth(), accountName(budget));
    }

    @Transactional
    public BudgetResponseDto update(User user, UUID id, UpdateBudgetRequestDto request) {
        Couple couple = getActiveCoupleOrThrow(user.getId());
        Budget budget = budgetRepository.findByIdAndCoupleId(id, couple.getId())
                .orElseThrow(BudgetNotFoundException::new);

        budget.setLimitAmount(request.limitAmount());
        Budget saved = budgetRepository.save(budget);
        return toResponse(saved, currentMonth(), accountName(saved));
    }

    @Transactional
    public void delete(User user, UUID id) {
        Couple couple = getActiveCoupleOrThrow(user.getId());
        Budget budget = budgetRepository.findByIdAndCoupleId(id, couple.getId())
                .orElseThrow(BudgetNotFoundException::new);
        budgetRepository.delete(budget);
    }

    private BudgetResponseDto toResponse(Budget budget, YearMonth month, String accountName) {
        OffsetDateTime from = month.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = month.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        BigDecimal spent = budget.getCategory() != null
                ? expenseRepository.sumByCategory(
                budget.getCoupleId(), budget.getCategory(), budget.getCurrency(), from, to)
                : expenseRepository.sumBySavingsAccount(
                budget.getCoupleId(), budget.getSavingsAccountId(), budget.getCurrency(), from, to);
        if (spent == null) spent = BigDecimal.ZERO;

        BigDecimal limit = budget.getLimitAmount();
        BigDecimal remaining = limit.subtract(spent).max(BigDecimal.ZERO);
        BigDecimal usagePercent = spent.multiply(HUNDRED).divide(limit, 2, RoundingMode.HALF_UP);

        return new BudgetResponseDto(
                budget.getId(),
                budget.getCategory(),
                budget.getSavingsAccountId(),
                accountName,
                limit,
                budget.getCurrency(),
                budget.getPeriod(),
                month.atDay(1),
                month.atEndOfMonth(),
                spent,
                remaining,
                usagePercent,
                spent.compareTo(limit) > 0
        );
    }

    private String accountName(Budget budget) {
        if (budget.getSavingsAccountId() == null) return null;
        return savingsRepository.findById(budget.getSavingsAccountId())
                .map(SavingsAccount::getName)
                .orElse(null);
    }

    private YearMonth currentMonth() {
        return YearMonth.now(ZoneOffset.UTC);
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