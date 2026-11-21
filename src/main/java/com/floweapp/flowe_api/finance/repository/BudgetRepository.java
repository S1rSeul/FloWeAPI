package com.floweapp.flowe_api.finance.repository;

import com.floweapp.flowe_api.finance.entity.Budget;
import com.floweapp.flowe_api.finance.entity.BudgetPeriod;
import com.floweapp.flowe_api.finance.entity.CurrencyCode;
import com.floweapp.flowe_api.finance.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findAllByCoupleIdOrderByCreatedAtAsc(UUID coupleId);

    Optional<Budget> findByIdAndCoupleId(UUID id, UUID coupleId);

    boolean existsByCoupleIdAndCategoryAndCurrencyAndPeriod(
            UUID coupleId, ExpenseCategory category, CurrencyCode currency, BudgetPeriod period);

    boolean existsBySavingsAccountIdAndPeriod(UUID savingsAccountId, BudgetPeriod period);
}