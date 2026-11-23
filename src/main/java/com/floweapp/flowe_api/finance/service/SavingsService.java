package com.floweapp.flowe_api.finance.service;

import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.exception.CoupleNotActiveException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.finance.dto.CreateSavingsAccountRequestDto;
import com.floweapp.flowe_api.finance.dto.DepositRequestDto;
import com.floweapp.flowe_api.finance.dto.SavingsAccountResponseDto;
import com.floweapp.flowe_api.finance.dto.UpdateSavingsAccountRequestDto;
import com.floweapp.flowe_api.finance.entity.Expense;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import com.floweapp.flowe_api.finance.entity.SavingsAccount;
import com.floweapp.flowe_api.finance.repository.ExpenseRepository;
import com.floweapp.flowe_api.finance.repository.SavingsAccountRepository;
import com.floweapp.flowe_api.task.exception.TaskNotFoundException;
import com.floweapp.flowe_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavingsService {

    private static final int MAX_ACCOUNTS_PER_COUPLE = 10;

    private final SavingsAccountRepository savingsRepository;
    private final ExpenseRepository expenseRepository;
    private final CoupleRepository coupleRepository;

    @Transactional
    public SavingsAccountResponseDto create(User user, CreateSavingsAccountRequestDto request) {
        Couple couple = getActiveCoupleOrThrow(user.getId());

        if (savingsRepository.countByCoupleId(couple.getId()) >= MAX_ACCOUNTS_PER_COUPLE) {
            throw new IllegalStateException("Достигнут лимит по количеству накопительных счетов на пару");
        }

        if (savingsRepository.existsByCoupleIdAndName(couple.getId(), request.name())) {
            throw new IllegalStateException("Накопительный счет с таким именем уже существует");
        }

        SavingsAccount account = SavingsAccount.builder()
                .coupleId(couple.getId())
                .name(request.name())
                .targetAmount(request.targetAmount())
                .currency(request.currency())
                .currentBalance(BigDecimal.ZERO)
                .build();

        return SavingsAccountResponseDto.from(savingsRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<SavingsAccountResponseDto> getAll(User user) {
        Couple couple = getActiveCoupleOrThrow(user.getId());
        return savingsRepository.findAllByCoupleIdOrderByCreatedAtAsc(couple.getId())
                .stream().map(SavingsAccountResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SavingsAccountResponseDto getById(User user, UUID taskId) {
        Couple couple = getActiveCoupleOrThrow(user.getId());

        SavingsAccount savingsAccount = savingsRepository.findByIdAndCoupleId(taskId, couple.getId())
                .orElseThrow(TaskNotFoundException::new);

        return SavingsAccountResponseDto.from(savingsAccount);
    }

    @Transactional
    public SavingsAccountResponseDto update(User user, UUID id, UpdateSavingsAccountRequestDto request) {
        Couple couple = getActiveCoupleOrThrow(user.getId());

        SavingsAccount account = savingsRepository.findByIdAndCoupleId(id, couple.getId())
                .orElseThrow(() -> new IllegalArgumentException("Накопительный счет не найден"));

        if (request.name() != null && !request.name().isNull()) {
            String newName = request.name().asString();
            if (!newName.equals(account.getName()) && savingsRepository.existsByCoupleIdAndName(couple.getId(), newName)) {
                throw new IllegalStateException("Накопительный счет с таким именем уже существует");
            }
            account.setName(newName);
        }

        if (request.targetAmount() != null) {
            account.setTargetAmount(request.targetAmount().isNull() ? null : request.targetAmount().decimalValue());
        }

        return SavingsAccountResponseDto.from(savingsRepository.save(account));
    }

    @Transactional
    public void delete(User user, UUID id) {
        Couple couple = getActiveCoupleOrThrow(user.getId());
        SavingsAccount account = savingsRepository.findByIdAndCoupleId(id, couple.getId())
                .orElseThrow(() -> new IllegalArgumentException("Накопительный счет не найден"));
        savingsRepository.delete(account);
    }

    @Transactional
    public SavingsAccountResponseDto deposit(User user, UUID id, DepositRequestDto request) {
        Couple couple = getActiveCoupleOrThrow(user.getId());
        boolean isCurrentUserUser1 = couple.getUser1Id().equals(user.getId());

        SavingsAccount account = savingsRepository.findByIdAndCoupleId(id, couple.getId())
                .orElseThrow(() -> new IllegalArgumentException("Накопительный счет не найден"));

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Размер депозита должен быть положительным");
        }

        account.setCurrentBalance(account.getCurrentBalance().add(request.amount()));
        savingsRepository.save(account);

        BigDecimal u1Paid = isCurrentUserUser1 ? request.amount() : BigDecimal.ZERO;
        BigDecimal u2Paid = isCurrentUserUser1 ? BigDecimal.ZERO : request.amount();

        Expense expense = Expense.builder()
                .coupleId(couple.getId())
                .createdById(user.getId())
                .savingsAccountId(account.getId())
                .amount(request.amount())
                .user1PaidAmount(u1Paid)
                .user2PaidAmount(u2Paid)
                .currency(account.getCurrency())
                .category(ExpenseCategory.SAVINGS)
                .spentAt(OffsetDateTime.now())
                .description("Пополнение копилки: " + account.getName())
                .build();

        expenseRepository.save(expense);

        return SavingsAccountResponseDto.from(account);
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
